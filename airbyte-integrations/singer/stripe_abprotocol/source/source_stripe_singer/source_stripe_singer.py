from airbyte_protocol import Source
from airbyte_protocol import AirbyteSpec
from airbyte_protocol import AirbyteCheckResponse
from airbyte_protocol import AirbyteCatalog
from airbyte_protocol import AirbyteMessage
from airbyte_protocol import AirbyteConfig
import requests
from typing import Generator
from base_singer import SingerHelper
import sys

class SourceStripeSinger(Source):
    def __init__(self):
        pass

    def spec(self) -> AirbyteSpec:
        return SingerHelper.spec_from_file('/airbyte/stripe-files/spec.json')

    def check(self, config: AirbyteConfig, rendered_config_path) -> AirbyteCheckResponse:
        json_config = config.json()
        r = requests.get('https://api.stripe.com/v1/customers', auth=(json_config['client_secret'], ''))

        return AirbyteCheckResponse(r.status_code == 200, {})

    def discover(self, config: AirbyteConfig, rendered_config_path) -> AirbyteCatalog:
        return SingerHelper.discover(f"tap-stripe --config {rendered_config_path} --discover")

    # todo: handle state
    def read(self, config: AirbyteConfig, rendered_config_path, state=None) -> Generator[AirbyteMessage, None, None]:
        return SingerHelper.read(f"tap-stripe --config {rendered_config_path}")
