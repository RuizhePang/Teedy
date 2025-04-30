# GRANT_TYPE=client_credentials
# CLIENT_ID=WJkOljacEYpVWNoIHjTG1ijn
# CLIENT_SECRET=4JMG8MLaxBftOGKDRfYTYxZjIv1FZlme
# URL=https://aip.baidubce.com/oauth/2.0/token
#
# curl -X POST \
#   -d "grant_type=${GRANT_TYPE}" \
#   -d "client_id=${CLIENT_ID}" \
#   -d "client_secret=${CLIENT_SECRET}" \
#   "${URL}"
#

ACCESS_TOKEN=24.4c85ec45ff500b87686b601c697352ac.2592000.1748572093.282335-118716685

# FROM="en"
# TO="ru"
# DOMAIN="general"
# INPUT_CONTENT=$(base64 ../../for_pre/test.pdf | tr -d '\n')
# INPUT_FORMAT="pdf"
# INPUT_FILENAME="测试文件.pdf"
# OUTPUT_FORMATS="pdf"
# OUTPUT_FILENAME_PREFIX="测试文件译文"
# URL=https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/create
#
# curl -H "Content-Type: application/json" \
#   -X POST \
#   -d "{
#     \"from\": \"${FROM}\",
#     \"to\": \"${TO}\",
#     \"domain\": \"general\",
#     \"input\": {
#       \"content\": \"${INPUT_CONTENT}\",
#       \"format\": \"${INPUT_FORMAT}\",
#       \"filename\": \"${INPUT_FILENAME}\"
#     },
#     \"output\": {
#       \"formats\": [\"${OUTPUT_FORMATS}\"],
#       \"filename_prefix\": \"${OUTPUT_FILENAME_PREFIX}\"
#     }
#   }" \
#   "https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/create?access_token=${ACCESS_TOKEN}"
#
#
URL=https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/get_result
ID=gp1eb8rjEVlQ8XvA3YZO

curl -H "Content-Type: application/json" \
 -X POST \
 -d "{
  \"id\": \"${ID}\"
  }" \
  "https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/query?access_token=${ACCESS_TOKEN}"

