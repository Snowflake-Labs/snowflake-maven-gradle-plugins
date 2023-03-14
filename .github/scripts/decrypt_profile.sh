#!/usr/bin/env bash

gpg --quiet --batch --yes --decrypt --passphrase="$PROFILE_PASSWORD" --output src/it/profile.properties profile.properties.gpg