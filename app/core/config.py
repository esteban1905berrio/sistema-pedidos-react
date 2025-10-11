"""Configuration management for SAP RFC connections."""

import os
from typing import Optional
from pydantic import BaseModel, Field
from dotenv import load_dotenv


class SAPConfig(BaseModel):
    """SAP connection configuration."""

    ashost: str = Field(description="Application server host address")
    sysnr: str = Field(description="System number (00-99)")
    client: str = Field(description="Client number (e.g., '100')")
    user: str = Field(description="SAP username")
    passwd: str = Field(description="SAP password")
    lang: str = Field(default="EN", description="Language code (e.g., 'EN', 'ES')")
    saprouter: Optional[str] = Field(default=None, description="SAP router string (optional)")

    class Config:
        frozen = True


def load_config() -> SAPConfig:
    """
    Load SAP configuration from environment variables.

    Expected environment variables:
    - SAP_ASHOST: Application server host
    - SAP_SYSNR: System number
    - SAP_CLIENT: Client number
    - SAP_USER: Username
    - SAP_PASSWD: Password
    - SAP_LANG: Language (optional, defaults to 'EN')
    - SAP_ROUTER: SAP router string (optional)

    Returns:
        SAPConfig: Configuration object

    Raises:
        ValueError: If required environment variables are missing
    """
    load_dotenv()

    required_vars = ["SAP_ASHOST", "SAP_SYSNR", "SAP_CLIENT", "SAP_USER", "SAP_PASSWD"]
    missing_vars = [var for var in required_vars if not os.getenv(var)]

    if missing_vars:
        raise ValueError(f"Missing required environment variables: {', '.join(missing_vars)}")

    return SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )
