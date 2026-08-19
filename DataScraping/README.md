# Data scraping

Extracts Primeira Liga match data from SofaScore into a canonical JSON format that
mirrors the database schema. It replaces an FBref scraper that stopped working when
FBref moved behind Cloudflare.

## Running it

```bash
cd DataScraping
python3 extract.py --season 2025/26 --matchday 1 --out round-1.json
python3 extract.py --season 2025/26 --matchday 1 --through 34 --out season.json
```

No dependencies: the standard library plus `curl`. Tests need `pytest`.

```bash
pip install pytest && python3 -m pytest tests -q
```

## Why it shells out to curl

Python's HTTP stack is refused. `requests` and `urllib` both receive 403 even when sent
a complete set of browser headers; `curl` receives 200 with no User-Agent at all. What is
filtered is the TLS fingerprint of the client, not what it claims to be. Shelling out to
curl keeps the client honest — it identifies itself as curl — and avoids depending on a
library whose purpose is to impersonate a browser.

This is an undocumented API and SofaScore's terms do not permit automated access. The
mitigation is architectural: `client.py` is the only module that knows how bytes arrive,
and `transform.py` is the only one that knows their shape, so replacing the source
touches two files and leaves the schema, the ingestion and the API untouched.

## Layout

| File | Role |
| --- | --- |
| `sofascore/client.py` | HTTP only: curl, one request at a time, paced and retried |
| `sofascore/transform.py` | Pure functions: source payloads to canonical records |
| `sofascore/validate.py` | Checks that refuse to emit a payload rather than emit a wrong one |
| `extract.py` | Command line: fetch a matchday, validate, write JSON |

## What validation is for

The previous scraper looked for columns named `Expected_xG` and `xA`, did not find them,
skipped them silently, and filled the database with unrelated per-90 figures that nobody
questioned for a year.

Checking that each field is present cannot work here, because SofaScore omits every
zero-valued key: in one real match only 2 of 40 players carried a `goals` key, and they
were the two scorers. An absent `goals` is indistinguishable from a player who did not
score.

So the checks run over the whole matchday instead. One player without a `goals` key is
ordinary; nobody in an entire matchday having one, while the scorelines say goals were
scored, can only mean the field is gone. Each match is also counted three ways from two
independent payloads — lineup statistics, goal incidents, and the scoreline — and they
have to agree.

When a check fails, `extract.py` writes nothing and exits non-zero, so a scheduled run
goes red rather than quietly ingesting nonsense.
