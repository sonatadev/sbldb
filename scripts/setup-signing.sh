#!/usr/bin/env bash
# One-time setup of sbl.db's permanent release key. Run it yourself on the VM:
#   bash scripts/setup-signing.sh
# It creates the key in ~/sbldb-private (outside the repo), stores it as GitHub secrets so
# the Android workflow can sign releases, and copies it to the OneDrive backup.
# Every APK from 0.17.0 on is signed with this key: if it's lost, updates stop installing
# over the old app, so keep the backup.
set -euo pipefail

dir="$HOME/sbldb-private"
jks="$dir/sbldb-release.jks"
repo="sonatadev/sbldb"

umask 077
mkdir -p "$dir"

if [ -e "$jks" ]; then
  echo "Key already there ($jks): reusing it."
  # shellcheck source=/dev/null
  . "$dir/keystore.env"
  pw="$SBLDB_KEYSTORE_PASSWORD"
else
  pw=$(openssl rand -hex 24)
  keytool -genkeypair -keystore "$jks" -storetype PKCS12 -alias sbldb \
    -keyalg RSA -keysize 4096 -validity 36500 \
    -storepass "$pw" -dname "CN=sbl.db, O=sonatadev"
  printf 'SBLDB_KEYSTORE_PASSWORD=%s\n' "$pw" > "$dir/keystore.env"
  cat > "$dir/README.txt" <<EOF
Release signing key for sbl.db (com.github.sonatadev.sbldb), created $(date +%F).
Every APK from 0.17.0 on is signed with it; losing it means users must uninstall to update.
Alias: sbldb. Store and key password: in keystore.env.
Copies: GitHub secrets SBLDB_KEYSTORE_B64 / SBLDB_KEYSTORE_PASSWORD on $repo,
and onedrive-backup:sbldb-private/ if rclone was set up.
EOF
  echo "Key created in $dir"
fi

base64 -w0 "$jks" | gh secret set SBLDB_KEYSTORE_B64 -R "$repo"
printf '%s' "$pw" | gh secret set SBLDB_KEYSTORE_PASSWORD -R "$repo"
echo "GitHub secrets set on $repo"

if rclone listremotes 2>/dev/null | grep -q '^onedrive-backup:$'; then
  rclone copy "$dir" onedrive-backup:sbldb-private/
  echo "Copied to onedrive-backup:sbldb-private/"
else
  echo "No onedrive-backup remote: copy $dir somewhere safe by hand."
fi
