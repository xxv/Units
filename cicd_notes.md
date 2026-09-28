CI/CD and Release notes
=======================

Releases
--------

Pushing a version tag (`v*`) builds a signed APK and a Play bundle and puts them
on a draft GitHub release:

    git tag v1.1 && git push origin v1.1

Signing uses these repository secrets, and the build falls back to unsigned
artifacts if they're missing. This is the current release key (a PKCS12
keystore, where the key password is the keystore password):

| Secret              | Value                            |
| ------------------- | -------------------------------- |
| `KEYSTORE_BASE64`   | keystore, base64 encoded         |
| `KEYSTORE_PASSWORD` | keystore password                |
| `KEY_ALIAS`         | key alias within the keystore    |
| `KEY_PASSWORD`      | password for that key            |

### Key rotation

Units was first released with a 1024-bit RSA key from 2009. Releases now use a
stronger key, and `signing/lineage` is the proof of rotation that lets Android
accept an update signed by the new key over an install signed by the old one.

- The **Play bundle** uses the current key, which is the Play upload key.
- The **APK** is signed with the current key and the lineage, as a v3 signature
  only, so it installs on Android 9 (API 28) and up: the first version that
  recognizes a rotated key. Signing for anything older would need the original
  key, which is never needed to produce a release. Android 6.0–8.1 installs come
  from Google Play.

`signing/sign-apk.sh` does the signing after the Gradle build, and
`./signing/sign-apk.sh check-lineage` (also run in CI) checks the lineage file.
To sign a release build locally, set `KEYSTORE_FILE` (a path), plus the same
passwords and alias, and run `./gradlew assembleRelease bundleRelease`. That
signs with the current key only; use the script for the lineage.
