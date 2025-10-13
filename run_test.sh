#!/bin/bash

# Set SAP RFC SDK environment variables
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH

# Run the test
.venv/bin/python "$@"
