#!/bin/bash
for psid in `ps x | grep "[r]miregistry $1"`; do kill $psid; break; done
