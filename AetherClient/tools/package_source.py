#!/usr/bin/env python3
"""Create a clean source archive and SHA-256 manifest for Aether Client."""

from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path


EXCLUDED_DIRECTORIES = {".gradle", "build", "saves"}


def main() -> int:
    project = Path(__file__).resolve().parents[1]
    output_dir = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else project.parent
    output_dir.mkdir(parents=True, exist_ok=True)
    archive_path = output_dir / "AetherClient_source_2026-11-10.zip"
    manifest_path = output_dir / "AetherClient_source_2026-11-10.manifest.txt"

    files = sorted(
        path
        for path in project.rglob("*")
        if path.is_file()
        and not any(part in EXCLUDED_DIRECTORIES for part in path.relative_to(project).parts)
    )

    manifest_rows = []
    for path in files:
        relative = path.relative_to(project).as_posix()
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        manifest_rows.append((relative, digest, path.stat().st_size))

    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path, (relative, _, _) in zip(files, manifest_rows):
            archive.write(path, relative)

    manifest = [
        "Aether Client source package — 2026-11-10",
        "Generated from the project directory; excludes build output, Gradle cache, and local world saves.",
        "SHA-256  SIZE(bytes)  PATH",
    ]
    for relative, digest, size in manifest_rows:
        manifest.append(f"{digest}  {size}  {relative}")
    manifest_path.write_text("\n".join(manifest) + "\n", encoding="utf-8")

    with zipfile.ZipFile(archive_path) as archive:
        names = set(archive.namelist())
    expected = {relative for relative, _, _ in manifest_rows}
    if names != expected:
        raise RuntimeError("Archive entries do not match the source manifest inputs")
    with zipfile.ZipFile(archive_path) as archive:
        for relative, expected_digest, expected_size in manifest_rows:
            data = archive.read(relative)
            actual_digest = hashlib.sha256(data).hexdigest()
            if len(data) != expected_size or actual_digest != expected_digest:
                raise RuntimeError(f"Archive verification failed for {relative}")

    print(f"Created {archive_path} ({len(files)} files, {archive_path.stat().st_size} bytes)")
    print(f"Created {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
