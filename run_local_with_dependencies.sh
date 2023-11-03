#!/bin/bash

sm2 -start MONGO

sm2 -start THIRD_PARTY_APPLICATION THIRD_PARTY_DEVELOPER

./run_local.sh
