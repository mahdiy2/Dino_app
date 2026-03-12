#!/usr/bin/env python3
"""
Quick Socket.IO E2E tester for messaging backend.

What it verifies:
1) Both users can connect with token auth.
2) Sender gets `message_sent` after emit.
3) Recipient gets `new_message` when online.
4) Recipient gets `pending_messages` when reconnecting after being offline.
5) `get_user_status` / `user_status` basic check.

Usage example:
python3 tools/socket_e2e_test.py \
  --server https://api.example.com \
  --sender-token "<JWT1>" \
  --sender-username alice \
  --receiver-token "<JWT2>" \
  --receiver-username bob

Dependencies:
pip install "python-socketio[client]"
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from dataclasses import dataclass, field
from typing import Any, Dict, List

import socketio


@dataclass
class ClientState:
    connected_payloads: List[Dict[str, Any]] = field(default_factory=list)
    message_sent_payloads: List[Dict[str, Any]] = field(default_factory=list)
    new_message_payloads: List[Dict[str, Any]] = field(default_factory=list)
    pending_messages_payloads: List[Dict[str, Any]] = field(default_factory=list)
    message_error_payloads: List[Dict[str, Any]] = field(default_factory=list)
    user_status_payloads: List[Dict[str, Any]] = field(default_factory=list)


def normalize_payload(data: Any) -> Dict[str, Any]:
    if isinstance(data, dict):
        return data
    if isinstance(data, str):
        try:
            return json.loads(data)
        except Exception:
            return {"raw": data}
    return {"raw": repr(data)}


def connect_client(name: str, server: str, token: str, state: ClientState) -> socketio.Client:
    sio = socketio.Client(logger=False, engineio_logger=False, reconnection=True)

    @sio.event
    def connect():
        print(f"[{name}] ✅ connected")

    @sio.event
    def disconnect():
        print(f"[{name}] ⚠️ disconnected")

    @sio.on("connected")
    def on_connected(data):
        p = normalize_payload(data)
        state.connected_payloads.append(p)
        print(f"[{name}] event connected => {p}")

    @sio.on("message_sent")
    def on_message_sent(data):
        p = normalize_payload(data)
        state.message_sent_payloads.append(p)
        print(f"[{name}] event message_sent => {p}")

    @sio.on("new_message")
    def on_new_message(data):
        p = normalize_payload(data)
        state.new_message_payloads.append(p)
        print(f"[{name}] event new_message => {p}")

    @sio.on("pending_messages")
    def on_pending_messages(data):
        p = normalize_payload(data)
        state.pending_messages_payloads.append(p)
        print(f"[{name}] event pending_messages => {p}")

    @sio.on("message_error")
    def on_message_error(data):
        p = normalize_payload(data)
        state.message_error_payloads.append(p)
        print(f"[{name}] event message_error => {p}")

    @sio.on("user_status")
    def on_user_status(data):
        p = normalize_payload(data)
        state.user_status_payloads.append(p)
        print(f"[{name}] event user_status => {p}")

    print(f"[{name}] connecting to {server} ...")
    sio.connect(
        server,
        auth={"token": token},
        transports=["websocket"],
        wait_timeout=10,
    )
    return sio


def wait_until(predicate, timeout_s: float = 10.0, step_s: float = 0.2) -> bool:
    end = time.time() + timeout_s
    while time.time() < end:
        if predicate():
            return True
        time.sleep(step_s)
    return False


def main() -> int:
    parser = argparse.ArgumentParser(description="Socket.IO messaging E2E test")
    parser.add_argument("--server", required=True, help="Socket server base URL, e.g. https://api.example.com")
    parser.add_argument("--sender-token", required=True)
    parser.add_argument("--sender-username", required=True)
    parser.add_argument("--receiver-token", required=True)
    parser.add_argument("--receiver-username", required=True)
    parser.add_argument("--message", default="socket-test-message")
    args = parser.parse_args()

    sender_state = ClientState()
    receiver_state = ClientState()

    sender = None
    receiver = None

    try:
        # 1) Connect both users
        receiver = connect_client("receiver", args.server, args.receiver_token, receiver_state)
        sender = connect_client("sender", args.server, args.sender_token, sender_state)

        # 2) Check status request path
        sender.emit("get_user_status", {"username": args.receiver_username})
        wait_until(lambda: len(sender_state.user_status_payloads) > 0, timeout_s=5)

        # 3) Online delivery test
        payload_online = {
            "to": args.receiver_username,
            "type": "text",
            "message": args.message,
            "encrypted_key": "test_encrypted_key",
        }
        print(f"[sender] emit send_message (online) => {payload_online}")
        sender.emit("send_message", payload_online)

        got_sender_ack_online = wait_until(lambda: len(sender_state.message_sent_payloads) >= 1, timeout_s=8)
        got_receiver_live = wait_until(lambda: len(receiver_state.new_message_payloads) >= 1, timeout_s=8)

        # 4) Offline queue test
        print("[receiver] disconnecting to test offline queue...")
        receiver.disconnect()
        time.sleep(1)

        payload_offline = {
            "to": args.receiver_username,
            "type": "text",
            "message": args.message + "-offline",
            "encrypted_key": "test_encrypted_key_offline",
        }
        print(f"[sender] emit send_message (offline) => {payload_offline}")
        sender.emit("send_message", payload_offline)

        got_sender_ack_offline = wait_until(lambda: len(sender_state.message_sent_payloads) >= 2, timeout_s=8)

        # reconnect receiver and expect pending_messages
        receiver = connect_client("receiver", args.server, args.receiver_token, receiver_state)
        got_receiver_pending = wait_until(
            lambda: any("messages" in p and isinstance(p["messages"], list) and len(p["messages"]) > 0 for p in receiver_state.pending_messages_payloads),
            timeout_s=10,
        )

        print("\n===== RESULT =====")
        print(f"sender_ack_online:   {got_sender_ack_online}")
        print(f"receiver_live_online:{got_receiver_live}")
        print(f"sender_ack_offline:  {got_sender_ack_offline}")
        print(f"receiver_pending:    {got_receiver_pending}")

        failed = False
        if not got_sender_ack_online:
            print("❌ sender did not receive message_sent for online send")
            failed = True
        if not got_receiver_live:
            print("❌ receiver did not receive new_message while online")
            failed = True
        if not got_sender_ack_offline:
            print("❌ sender did not receive message_sent for offline send")
            failed = True
        if not got_receiver_pending:
            print("❌ receiver did not receive pending_messages after reconnect")
            failed = True

        if sender_state.message_error_payloads:
            print(f"❌ sender message_error events => {sender_state.message_error_payloads}")
            failed = True
        if receiver_state.message_error_payloads:
            print(f"❌ receiver message_error events => {receiver_state.message_error_payloads}")
            failed = True

        if failed:
            return 1

        print("✅ socket messaging flow looks healthy")
        return 0

    except Exception as e:
        print(f"❌ test crashed: {e}")
        return 2
    finally:
        try:
            if sender is not None and sender.connected:
                sender.disconnect()
        except Exception:
            pass
        try:
            if receiver is not None and receiver.connected:
                receiver.disconnect()
        except Exception:
            pass


if __name__ == "__main__":
    sys.exit(main())
