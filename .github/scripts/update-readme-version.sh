#!/bin/bash

#
# Copyright 2024 Kazimierz Pogoda / Xemantic
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Ensure VERSION environment variable is set
if [ -z "$VERSION" ]; then
    echo "Error: VERSION environment variable is not set"
    exit 1
fi

# Check if settings.gradle.kts exists
if [ ! -f "settings.gradle.kts" ]; then
    echo "Error: settings.gradle.kts not found"
    exit 1
fi

# Extract groupId and name from settings.gradle.kts
GROUP_ID=$(grep "val groupId = " settings.gradle.kts | sed -n 's/.*groupId = "\(.*\)".*/\1/p')
ARTIFACT_ID=$(grep "val name = " settings.gradle.kts | sed -n 's/.*name = "\(.*\)".*/\1/p')

if [ -z "$GROUP_ID" ] || [ -z "$ARTIFACT_ID" ]; then
    echo "Error: Could not extract groupId or name from settings.gradle.kts"
    exit 1
fi

# Check if README.md exists
if [ ! -f "README.md" ]; then
    echo "Error: README.md not found"
    exit 1
fi

# Escape special characters in the group ID for sed
ESCAPED_GROUP_ID=$(echo "$GROUP_ID" | sed 's/\./\\./g')

# Create the pattern to match
PATTERN="\"$ESCAPED_GROUP_ID:$ARTIFACT_ID:[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\""

# Create the replacement string
REPLACEMENT="\"$GROUP_ID:$ARTIFACT_ID:$VERSION\""

# Check if the pattern exists in the file
if ! grep -q "$GROUP_ID:$ARTIFACT_ID:[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*" README.md; then
    echo "Error: Dependency pattern not found in README.md"
    exit 1
fi

# Perform the replacement and save to a temporary file
sed "s|$PATTERN|$REPLACEMENT|g" README.md > README.md.tmp

# Check if sed made any changes
if cmp -s README.md README.md.tmp; then
    echo "No version updates were needed"
    rm README.md.tmp
    exit 0
fi

# Move the temporary file back to the original
mv README.md.tmp README.md

echo "Successfully updated version to $VERSION in README.md"
