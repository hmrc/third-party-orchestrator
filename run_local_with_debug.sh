#!/bin/bash

sbt -jvm-debug 5005 "run -Dhttp.port=15505 -Dmicroservice.services.api-platform-events.enabled=false$*"
