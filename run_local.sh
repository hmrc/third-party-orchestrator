#!/bin/bash

sbt "~run -Drun.mode=Dev -Dhttp.port=15501 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes $*"
