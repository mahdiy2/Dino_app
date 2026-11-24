"""Run from the backend root with its virtualenv and PYTHONPATH=. set."""
import os
import sys
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from tests.test_message_socket import TestSocketMessageFlow


def main():
    tokens_path = Path(sys.argv[1])
    with TemporaryDirectory(prefix="dino-realtime-") as temporary:
        from cryptography.hazmat.primitives import serialization
        from cryptography.hazmat.primitives.asymmetric import rsa

        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        public_key = key.public_key().public_bytes(
            serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo,
        ).decode()

        # The older fixture uses placeholder keys. Supply valid RSA keys for the current auth service.
        os.environ["DATABASE_URL"] = f"sqlite:///{temporary}/queue-test.db"
        os.environ["JWT_SECRET_KEY"] = "local-socket-integration-fixture-secret"
        from app.services import auth_service
        from flask_jwt_extended import create_access_token

        register = auth_service.register
        fixture = TestSocketMessageFlow
        with patch.object(auth_service, "register", lambda user, password, _: register(user, password, public_key)):
            fixture.setUpClass()
        try:
            with fixture.app.app_context():
                refreshed = create_access_token(identity="alice")
            tokens_path.write_text(f"{fixture.alice_token}\n{refreshed}\n")
            fixture.socketio.run(fixture.app, host="127.0.0.1", port=19381, allow_unsafe_werkzeug=True)
        finally:
            fixture.tearDownClass()


if __name__ == "__main__":
    main()
