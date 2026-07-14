# OpenTheDoor APK scan report

## URLs found

- `http://gamejoy-public.s3-website-us-east-1.amazonaws.com/gamejoy/pictures/` - LOW - CDN/resource host

## Domain names

- `gamejoy-public.s3-website-us-east-1.amazonaws.com`

## Possible access and config paths

- `AndroidManifest.xml`
- `assets/AllInOne/info.dat`
- `/gm/index.jsp`
- `/gamejoy/pictures/`

## Protocol hints

- `amf`
- `api`
- `auth`
- `cdn`
- `cdn/resource download`
- `connect`
- `endpoint`
- `host`
- `http`
- `login`
- `port`
- `server`
- `socket`
- `swf/flash`
- `websocket`

## Suggested hosts entries

No reliable host entry to suggest. Start with `tcp-listen` or patch the client toward `127.0.0.1`.

## Suggested OpenTheDoor config

```properties
mode=http-listen
host=0.0.0.0
port=80
mockDir=mocks
savePackets=true
logDir=logs
```

Reason: based on `http://gamejoy-public.s3-website-us-east-1.amazonaws.com/gamejoy/pictures/`.
