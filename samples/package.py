import glob
import json
import os
import sys
import zipfile

SAMPLE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "basic")
TEMPLATES = (".scm", ".bky", ".properties")


def package(aix_path, aia_path):
    with zipfile.ZipFile(aix_path) as aix, zipfile.ZipFile(aia_path, "w", zipfile.ZIP_DEFLATED) as aia:
        component = next(json.loads(aix.read(n))[0] for n in aix.namelist() if n.endswith("/components.json"))
        for root, _, files in os.walk(SAMPLE):
            for name in files:
                path = os.path.join(root, name)
                if name.endswith(TEMPLATES):
                    content = open(path, encoding="utf-8").read().replace("{{MEDIA3_VERSION}}", str(component["version"]))
                    aia.writestr(os.path.relpath(path, SAMPLE), content)
                else:
                    aia.write(path, os.path.relpath(path, SAMPLE))
        for name in aix.namelist():
            if not name.endswith("/"):
                aia.writestr(f"assets/external_comps/{name}", aix.read(name))


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "out"
    package(glob.glob(os.path.join(out, "*.aix"))[0], os.path.join(out, "Media3Sample.aia"))
