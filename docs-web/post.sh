ACCESS_TOKEN=24.0de9e87730bb10a866bf3dd3d1ac3e9d.2592000.1748674788.282335-118716685

FROM=auto
TO=zh
DOMAIN=general
INPUT_CONTENT=$(base64 "../../for_pre/test.pdf")
INPUT_FORMAT=pdf
INPUT_FILENAME="test.pdf"
OUTPUT_FORMATS=pdf
OUTPUT_FILENAME_PREFIX="translated"

URL=https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/create

curl -H "Content-Type: application/json" \
  -X POST \
  -d '{
    "from": "'"$FROM"'",
    "to": "'"$TO"'",
    "domain": "'"$DOMAIN"'",
    "input": {
      "content": "'"$INPUT_CONTENT"'",
      "format": "'"$INPUT_FORMAT"'",
      "filename": "'"$INPUT_FILENAME"'"
    },
    "output": {
      "formats": ["'"$OUTPUT_FORMATS"'"],
      "filename_prefix": "'"$OUTPUT_FILENAME_PREFIX"'"
    }
  }' "$URL?access_token=$ACCESS_TOKEN"
#
ID=EVP3kqr1agD4PrlQ9GdO
URL=https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/query

curl -H "Content-Type: application/json" \
  -X POST \
  -d '{
    "id": "'"$ID"'"
  }' "$URL?access_token=$ACCESS_TOKEN"

