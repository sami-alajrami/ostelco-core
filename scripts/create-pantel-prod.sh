for LOCATION in $(find . -name .gitignore  -exec grep pantel-prod.json  '{}' '+' ); do 
    DIR_NAME=$(dirname $LOCATION)
    echo "Creating secrets file: ${DIR_NAME}/pantel-prod.json ..."
    echo ${PANTEL_SECRETS_FILE} | base64 -d >  ${DIR_NAME}/pantel-prod.json
    ls -l ${DIR_NAME}/pantel-prod.json 
done