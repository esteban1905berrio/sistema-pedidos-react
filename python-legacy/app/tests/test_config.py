"""Unit tests for configuration management."""

import pytest
import os
from unittest.mock import patch
from app.core.config import SAPConfig, load_config


class TestSAPConfig:
    """Test SAPConfig model."""

    def test_config_creation_with_required_fields(self):
        """Test creating config with all required fields."""
        config = SAPConfig(
            ashost="172.27.154.8",
            sysnr="00",
            client="100",
            user="testuser",
            passwd="testpass",
        )

        assert config.ashost == "172.27.154.8"
        assert config.sysnr == "00"
        assert config.client == "100"
        assert config.user == "testuser"
        assert config.passwd == "testpass"
        assert config.lang == "EN"  # Default value
        assert config.saprouter is None  # Optional field

    def test_config_with_all_fields(self):
        """Test creating config with all fields including optional ones."""
        config = SAPConfig(
            ashost="172.27.154.8",
            sysnr="00",
            client="100",
            user="testuser",
            passwd="testpass",
            lang="ES",
            saprouter="/H/190.145.188.150/S/sapdp99",
        )

        assert config.lang == "ES"
        assert config.saprouter == "/H/190.145.188.150/S/sapdp99"

    def test_config_is_immutable(self):
        """Test that config is frozen (immutable)."""
        config = SAPConfig(
            ashost="172.27.154.8",
            sysnr="00",
            client="100",
            user="testuser",
            passwd="testpass",
        )

        with pytest.raises(Exception):  # Pydantic raises ValidationError for frozen models
            config.ashost = "new_host"


class TestLoadConfig:
    """Test load_config function."""

    @patch.dict(
        os.environ,
        {
            "SAP_ASHOST": "172.27.154.8",
            "SAP_SYSNR": "00",
            "SAP_CLIENT": "100",
            "SAP_USER": "testuser",
            "SAP_PASSWD": "testpass",
        },
        clear=True,
    )
    def test_load_config_with_required_vars(self):
        """Test loading config with only required environment variables."""
        config = load_config()

        assert config.ashost == "172.27.154.8"
        assert config.sysnr == "00"
        assert config.client == "100"
        assert config.user == "testuser"
        assert config.passwd == "testpass"
        assert config.lang == "EN"  # Default
        assert config.saprouter is None

    @patch.dict(
        os.environ,
        {
            "SAP_ASHOST": "172.27.154.8",
            "SAP_SYSNR": "00",
            "SAP_CLIENT": "100",
            "SAP_USER": "testuser",
            "SAP_PASSWD": "testpass",
            "SAP_LANG": "ES",
            "SAP_ROUTER": "/H/190.145.188.150/S/sapdp99",
        },
        clear=True,
    )
    def test_load_config_with_all_vars(self):
        """Test loading config with all environment variables."""
        config = load_config()

        assert config.lang == "ES"
        assert config.saprouter == "/H/190.145.188.150/S/sapdp99"

    @patch.dict(os.environ, {"SAP_ASHOST": "172.27.154.8"}, clear=True)
    def test_load_config_missing_required_vars(self):
        """Test that loading config fails with missing required variables."""
        with pytest.raises(ValueError) as excinfo:
            load_config()

        error_message = str(excinfo.value)
        assert "Missing required environment variables" in error_message
        assert "SAP_SYSNR" in error_message
        assert "SAP_CLIENT" in error_message
        assert "SAP_USER" in error_message
        assert "SAP_PASSWD" in error_message

    @patch.dict(os.environ, {}, clear=True)
    def test_load_config_no_vars(self):
        """Test that loading config fails with no environment variables."""
        with pytest.raises(ValueError) as excinfo:
            load_config()

        error_message = str(excinfo.value)
        assert "Missing required environment variables" in error_message
