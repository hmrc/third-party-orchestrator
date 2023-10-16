#!/bin/bash

sbt -jvm-debug 5005 "run -Dhttp.port=15501 -Dmicroservice.services.api-platform-events.enabled=false$*"
