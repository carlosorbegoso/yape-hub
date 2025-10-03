#!/bin/bash

# Script para actualizar todas las llamadas make_request en test_all_apis.sh

echo "Actualizando llamadas make_request para guardar respuestas..."

# Actualizar llamadas específicas
sed -i '' 's/make_request "POST" "$BASE_URL\/api\/auth\/seller\/login-by-phone?phone=$SELLER_PHONE&affiliationCode=$AFFILIATION_CODE" ""/make_request "POST" "$BASE_URL\/api\/auth\/seller\/login-by-phone?phone=$SELLER_PHONE&affiliationCode=$AFFILIATION_CODE" "" "" "seller_login"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "GET" "$BASE_URL\/api\/admin\/profile?userId=$ADMIN_ID" "" "$auth_header"/make_request "GET" "$BASE_URL\/api\/admin\/profile?userId=$ADMIN_ID" "" "$auth_header" "admin_profile"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "PUT" "$BASE_URL\/api\/admin\/profile?userId=$ADMIN_ID" "$update_data" "$auth_header"/make_request "PUT" "$BASE_URL\/api\/admin\/profile?userId=$ADMIN_ID" "$update_data" "$auth_header" "admin_update_profile"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "POST" "$BASE_URL\/api\/admin\/branches" "$branch_data" "$auth_header"/make_request "POST" "$BASE_URL\/api\/admin\/branches" "$branch_data" "$auth_header" "create_branch"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "GET" "$BASE_URL\/api\/admin\/branches?page=0&size=20&status=all" "" "$auth_header"/make_request "GET" "$BASE_URL\/api\/admin\/branches?page=0&size=20&status=all" "" "$auth_header" "list_branches"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "GET" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID" "" "$auth_header"/make_request "GET" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID" "" "$auth_header" "get_branch"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "PUT" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID" "$update_branch_data" "$auth_header"/make_request "PUT" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID" "$update_branch_data" "$auth_header" "update_branch"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "GET" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID\/sellers?page=0&size=20" "" "$auth_header"/make_request "GET" "$BASE_URL\/api\/admin\/branches\/$BRANCH_ID\/sellers?page=0&size=20" "" "$auth_header" "branch_sellers"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "POST" "$BASE_URL\/api\/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header"/make_request "POST" "$BASE_URL\/api\/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header" "generate_affiliation_code"/g' doc/test_all_apis.sh

sed -i '' 's/make_request "POST" "$BASE_URL\/api\/validate-affiliation-code" "$validate_data"/make_request "POST" "$BASE_URL\/api\/validate-affiliation-code" "$validate_data" "" "validate_affiliation_code"/g' doc/test_all_apis.sh

echo "Actualizaciones completadas!"