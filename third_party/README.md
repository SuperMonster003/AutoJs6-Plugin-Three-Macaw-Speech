# Third-party data

Upstream files kept byte for byte (`.gitattributes` marks them `-text`). `.python/generate_kws_pinyin.py` derives the
keyword pinyin table of the APK (`app/src/main/assets/kws/`) from them; it refuses files whose SHA-256 differs from the
table, and `--check` verifies that the generated files match.

| Path | Upstream | Pinned commit | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `pinyin-data/pinyin.txt` | https://github.com/mozillazg/pinyin-data (`pinyin.txt`: readings of single characters, the customary reading first) | `9193766130af24d2ac54230be979b2e98ac66223` | MIT (`pinyin-data/LICENSE`) | `621f8ca9eff8519f47e2b17b564fd318161e13bca07eea8c8e04993cd5d3b52e` |

License text: `pinyin-data/LICENSE` (`9c048697be2502a16e8bcb282d5d465a07295b2def0ffb05a269c5d39dbe1586`).

The same two files, at the same commit and with the same digests, are staged in AutoJs6-Plugin-Batch-Rename
(`third_party/pinyin-data/`), from where they were copied on 2026-10-16.

Why a table of its own (roadmap D18, appendix D Q4): sherpa-onnx 1.13.8 accepts keywords only as model tokens; its JNI
library carries no pinyin conversion. The plugin spells every Chinese character of a keyword with its customary
reading from this table and splits the syllable into the initial and the tonal final of the zh-en keyword spotter
(`phone+ppinyin`, pypinyin with `strict=False`); English words come from the model's own `en.phone` lexicon.

## License texts

The About page shows these texts from the APK (roadmap D32, P5.2); the build copies them, together with `LICENSE` and
`THIRD_PARTY_NOTICES.md`, into the generated `assets/legal/` folder and refuses other bytes (`locks/legal-texts.lock`).
Both were downloaded on 2026-10-16 with the maintainer's consent.

| Path | Upstream | License | SHA-256 |
| --- | --- | --- | --- |
| `licenses/GPL-3.0.txt` | https://www.gnu.org/licenses/gpl-3.0.txt | the text itself (verbatim copies allowed) | `3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986` |
| `licenses/COPYING.UCD` | `src/ucd-tools/COPYING.UCD` of https://github.com/csukuangfj/espeak-ng at `ed530aa113046142eb5115cf2fc9157854d0ffe1` | Unicode Terms of Use | `be029c50df83105a810e391778b6edcb522d6cb4b4e142332aca54437a499bf7` |
