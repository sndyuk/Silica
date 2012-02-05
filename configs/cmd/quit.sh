#!/bin/bash
for psid in `ps x | grep "[r]miregistry $1"`; do kill $psid; break; done

for psid in `ps x | grep "[j]ava" | grep "com.silica.Silica"`; do kill $psid; break; done

echo "rmiregistry killed."

exit 0