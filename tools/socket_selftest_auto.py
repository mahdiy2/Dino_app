#!/usr/bin/env python3
"""
Linux-friendly one-command Socket.IO self-test.

What this script does automatically:
1) Creates two random users via /api/auth/register
2) Logs them in via /api/auth/login to get access tokens
3) Connects both users to Socket.IO with auth token
4) Tests online delivery: sender -> receiver (message_sent + new_message)
5) Tests offline queue: receiver disconnects, sender sends, receiver reconnects (pending_messages)
6) Tests get_user_status / user_status

Usage:
  python3 tools/socket_selftest_auto.py --server http://127.0.0.1:5000

Notes:
- Requires Python 3.
- Requires python-socketio client package. The script attempts to auto-install it if missing.
"""

from __future__ import annotations

import argparse
import importlib
import json
import random
import string
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Any, Dict, List, Tuple


def ensure_socketio_installed() -> None:
    try:
        importlib.import_module("socketio")
        return
    except Exception:
        pass

    print("[setup] python-socketio not found. Trying to install automatically...")
    cmd = [sys.executable, "-m", "pip", "install", "python-socketio[client]"]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        print("[setup] ❌ failed to install python-socketio")
        print(proc.stdout)
        print(proc.stderr)
        sys.exit(2)

    try:
        importlib.import_module("socketio")
    except Exception as e:
        print(f"[setup] ❌ install seemed successful but import failed: {e}")
        sys.exit(2)


def rand_str(n: int = 8) -> str:
    chars = string.ascii_lowercase + string.digits
    return "".join(random.choice(chars) for _ in range(n))


def base_url(server: str) -> str:
    return server.rstrip("/")


def post_json(url: str, payload: Dict[str, Any]) -> Tuple[int, Dict[str, Any]]:
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            raw = resp.read().decode("utf-8")
            return resp.getcode(), json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            data = json.loads(raw) if raw else {}
        except Exception:
            data = {"raw": raw}
        return e.code, data


def register_user(server: str, username: str, password: str) -> None:
    url = f"{base_url(server)}/api/auth/register"
    payload = {
        "username": username,
        "password": password,
        "public_key": f"TEST_PUBLIC_KEY_{username}_{rand_str(6)}",
    }
    code, data = post_json(url, payload)
    if code not in (200, 201):
        raise RuntimeError(f"register failed for {username}: HTTP {code} => {data}")
    print(f"[auth] ✅ register {username}: {data}")


def login_user(server: str, username: str, password: str) -> str:
    url = f"{base_url(server)}/api/auth/login"
    payload = {"username": username, "password": password}
    code, data = post_json(url, payload)
    if code != 200:
        raise RuntimeError(f"login failed for {username}: HTTP {code} => {data}")
    token = data.get("access_token")
    if not token:
        raise RuntimeError(f"login response missing access_token for {username}: {data}")
    print(f"[auth] ✅ login {username}")
    return token


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


def wait_until(predicate, timeout_s: float = 10.0, step_s: float = 0.2) -> bool:
    end = time.time() + timeout_s
    while time.time() < end:
        if predicate():
            return True
        time.sleep(step_s)
    return False


def connect_client(name: str, server: str, token: str, state: ClientState):
    import socketio  # imported after ensure_socketio_installed

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
        wait_timeout=12,
    )
    return sio


def main() -> int:
    parser = argparse.ArgumentParser(description="Auto register + login + socket self-test")
    parser.add_argument("--server", default="http://127.0.0.1:5000", help="API base URL")
    parser.add_argument("--password", default="Test@123456", help="password for generated users")
    parser.add_argument("--message", default="auto-socket-test-message")
    args = parser.parse_args()

    ensure_socketio_installed()

    suffix = rand_str(10)
    sender_username = f"sock_sender_{suffix}"
    receiver_username = f"sock_receiver_{suffix}"

    sender_state = ClientState()
    receiver_state = ClientState()

    sender = None
    receiver = None

    try:
        print("\n=== STEP 1: register users ===")
        register_user(args.server, sender_username, args.password)
        register_user(args.server, receiver_username, args.password)

        print("\n=== STEP 2: login users ===")
        sender_token = login_user(args.server, sender_username, args.password)
        receiver_token = login_user(args.server, receiver_username, args.password)

        print("\n=== STEP 3: connect both sockets ===")
        receiver = connect_client("receiver", args.server, receiver_token, receiver_state)
        sender = connect_client("sender", args.server, sender_token, sender_state)

        print("\n=== STEP 4: user status check ===")
        sender.emit("get_user_status", {"username": receiver_username})
        got_status = wait_until(lambda: len(sender_state.user_status_payloads) > 0, timeout_s=6)

        print("\n=== STEP 5: online delivery test ===")
        payload_online = {
            "to": receiver_username,
            "type": "text",
            "message": args.message,
            "encrypted_key": "test_encrypted_key_online",
        }
        print(f"[sender] emit send_message => {payload_online}")
        sender.emit("send_message", payload_online)

        got_sender_ack_online = wait_until(lambda: len(sender_state.message_sent_payloads) >= 1, timeout_s=8)
        got_receiver_live = wait_until(lambda: len(receiver_state.new_message_payloads) >= 1, timeout_s=8)

        print("\n=== STEP 6: offline queue test ===")
        receiver.disconnect()
        time.sleep(1)
        payload_offline = {
            "to": receiver_username,
            "type": "text",
            "message": args.message + "-offline",
            "encrypted_key": "test_encrypted_key_offline",
        }
        print(f"[sender] emit send_message while receiver offline => {payload_offline}")
        sender.emit("send_message", payload_offline)

        got_sender_ack_offline = wait_until(lambda: len(sender_state.message_sent_payloads) >= 2, timeout_s=8)

        receiver = connect_client("receiver", args.server, receiver_token, receiver_state)
        got_receiver_pending = wait_until(
            lambda: any(
                "messages" in p and isinstance(p.get("messages"), list) and len(p["messages"]) > 0
                for p in receiver_state.pending_messages_payloads
            ),
            timeout_s=10,
        )

        print("\n===== FINAL RESULT =====")
        print(f"sender_user:          {sender_username}")
        print(f"receiver_user:        {receiver_username}")
        print(f"user_status_received: {got_status}")
        print(f"sender_ack_online:    {got_sender_ack_online}")
        print(f"receiver_live_online: {got_receiver_live}")
        print(f"sender_ack_offline:   {got_sender_ack_offline}")
        print(f"receiver_pending:     {got_receiver_pending}")

        failed = False
        if not got_status:
            print("❌ user_status not received")
            failed = True
        if not got_sender_ack_online:
            print("❌ sender did not receive message_sent for online send")
            failed = True
        if not got_receiver_live:
            print("❌ receiver did not receive new_message while online")
            failed = True
        if not got_sender_ack_offline:
            print("❌ sender did not receive message_sent while receiver offline")
            failed = True
        if not got_receiver_pending:
            print("❌ receiver did not receive pending_messages after reconnect")
            failed = True

        if sender_state.message_error_payloads:
            print(f"❌ sender message_error => {sender_state.message_error_payloads}")
            failed = True
        if receiver_state.message_error_payloads:
            print(f"❌ receiver message_error => {receiver_state.message_error_payloads}")
            failed = True

        if failed:
            return 1

        print("✅ all socket checks passed")
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
