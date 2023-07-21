## Building, signing, notarizing McIDAS-V Installers

Apple's requires us to submit our macOS installers from a macOS machine, so this means
that our nightly build process now involves two machines.

### TL;DR

1. Create the `~/.config/mcidasv_install4j/` directory.

2. Use either the macOS or Linux env template from this directory to create `~/.config/mcidasv_install4j/install4j.env`.

3. Optionally put `mcv_install4j` into `~/bin` or some other convenient place.

4. Make sure the macOS machine can scp the installers into `~/install4j/media` on the Linux machine.

5. Run `mcv_install4j` from the macOS machine and make sure the installers that (should) wind up in `~/install4j/media` on the Linux machine are actually signed and notarized.

6. Run `mcv_install4j` from the Linux machine to make sure the Linux and Windows installers work. The Windows installers should also be signed automatically.

7. Make sure the scripts running on the webserver can actually rsync the installers from the Linux machine's `~/install4j/media` directory.

8. Relax!

