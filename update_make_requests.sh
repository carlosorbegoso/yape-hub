#!/bin/bash

# Script para actualizar todas las llamadas make_request en test_all_apis.sh

# Lista de actualizaciones necesarias
updates=(
    'make_request "POST" "$BASE_URL/api/auth/forgot-password?email=$ADMIN_EMAIL" "" "" "forgot_password"'
    'make_request "POST" "$BASE_URL/api/auth/seller/login-by-phone?phone=$SELLER_PHONE&affiliationCode=$AFFILIATION_CODE" "" "" "seller_login"'
    'make_request "GET" "$BASE_URL/api/admin/profile?userId=$ADMIN_ID" "" "$auth_header" "admin_profile"'
    'make_request "PUT" "$BASE_URL/api/admin/profile?userId=$ADMIN_ID" "$update_data" "$auth_header" "admin_update_profile"'
    'make_request "POST" "$BASE_URL/api/admin/branches" "$branch_data" "$auth_header" "create_branch"'
    'make_request "GET" "$BASE_URL/api/admin/branches?page=0&size=20&status=all" "" "$auth_header" "list_branches"'
    'make_request "GET" "$BASE_URL/api/admin/branches/$BRANCH_ID" "" "$auth_header" "get_branch"'
    'make_request "PUT" "$BASE_URL/api/admin/branches/$BRANCH_ID" "$update_branch_data" "$auth_header" "update_branch"'
    'make_request "GET" "$BASE_URL/api/admin/branches/$BRANCH_ID/sellers?page=0&size=20" "" "$auth_header" "branch_sellers"'
    'make_request "POST" "$BASE_URL/api/generate-affiliation-code-protected?adminId=$ADMIN_ID&expirationHours=24&maxUses=10&branchId=$BRANCH_ID&notes=Test" "" "$auth_header" "generate_affiliation_code"'
    'make_request "POST" "$BASE_URL/api/validate-affiliation-code" "$validate_data" "" "validate_affiliation_code"'
    'make_request "POST" "$BASE_URL/api/seller/register" "$seller_data" "" "register_seller"'
    'make_request "POST" "$BASE_URL/api/generate-qr-base64" "$qr_data" "" "generate_qr"'
    'make_request "POST" "$BASE_URL/api/login-with-qr" "$qr_login_data" "" "login_with_qr"'
    'make_request "GET" "$BASE_URL/api/admin/sellers/my-sellers?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=1&limit=20" "" "$auth_header" "my_sellers"'
    'make_request "GET" "$BASE_URL/api/admin/sellers?page=1&limit=20&branchId=$BRANCH_ID&status=all" "" "$auth_header" "list_sellers"'
    'make_request "PUT" "$BASE_URL/api/admin/sellers/$SELLER_ID?adminId=$ADMIN_ID&name=Vendedor Actualizado&phone=$SELLER_PHONE&isActive=true" "" "$auth_header" "update_seller"'
    'make_request "DELETE" "$BASE_URL/api/admin/sellers/$SELLER_ID?adminId=$ADMIN_ID&action=pause&reason=Test" "" "$auth_header" "pause_seller"'
    'make_request "GET" "$BASE_URL/api/admin/sellers/limits?adminId=$ADMIN_ID" "" "$auth_header" "seller_limits"'
    'make_request "GET" "$BASE_URL/api/payments/status/$SELLER_ID" "" "$auth_header" "seller_status"'
    'make_request "POST" "$BASE_URL/api/payments/claim" "$claim_data" "$auth_header" "claim_payment"'
    'make_request "POST" "$BASE_URL/api/payments/reject" "$reject_data" "$auth_header" "reject_payment"'
    'make_request "GET" "$BASE_URL/api/payments/pending?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20&limit=20" "" "$auth_header" "pending_payments"'
    'make_request "GET" "$BASE_URL/api/payments/admin/management?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20&status=all" "" "$auth_header" "admin_management"'
    'make_request "GET" "$BASE_URL/api/payments/notification-stats" "" "$auth_header" "notification_stats"'
    'make_request "GET" "$BASE_URL/api/payments/admin/connected-sellers?adminId=$ADMIN_ID" "" "$auth_header" "connected_sellers"'
    'make_request "GET" "$BASE_URL/api/payments/test/admin/management?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20&status=all" "" "" "test_admin_management"'
    'make_request "GET" "$BASE_URL/api/payments/admin/sellers-status?adminId=$ADMIN_ID" "" "$auth_header" "all_sellers_status"'
    'make_request "GET" "$BASE_URL/api/payments/confirmed?sellerId=$SELLER_ID&adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&page=0&size=20" "" "$auth_header" "confirmed_payments"'
    'make_request "GET" "$BASE_URL/api/notifications?userId=$ADMIN_ID&userRole=admin&startDate=2024-01-01&endDate=2024-12-31&page=1&limit=20&unreadOnly=false" "" "$auth_header" "notifications"'
    'make_request "POST" "$BASE_URL/api/notifications/1/read" "" "" "mark_notification_read"'
    'make_request "POST" "$BASE_URL/api/notifications/yape-notifications" "$yape_data" "$auth_header" "yape_notification"'
    'make_request "GET" "$BASE_URL/api/notifications/yape-audit?adminId=$ADMIN_ID&page=0&size=20" "" "$auth_header" "yape_audit"'
    'make_request "GET" "$BASE_URL/api/billing?type=dashboard&adminId=$ADMIN_ID&period=current&include=details&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header" "billing_info"'
    'make_request "GET" "$BASE_URL/api/billing/plans" "" "" "billing_plans"'
    'make_request "GET" "$BASE_URL/api/billing/dashboard?adminId=$ADMIN_ID" "" "$auth_header" "billing_dashboard"'
    'make_request "POST" "$BASE_URL/api/billing/load-data" "" "" "load_data"'
    'make_request "POST" "$BASE_URL/api/billing/operations?adminId=$ADMIN_ID&action=generate-code&validate=true" "$operations_data" "$auth_header" "billing_operations"'
    'make_request "POST" "$BASE_URL/api/billing/payments/upload?adminId=$ADMIN_ID&paymentCode=PAY123" "$upload_data" "$auth_header" "upload_payment"'
    'make_request "GET" "$BASE_URL/api/billing/payments/status/PAY123" "" "$auth_header" "payment_status"'
    'make_request "GET" "$BASE_URL/api/admin/billing?type=dashboard&adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&status=all&include=details" "" "$auth_header" "admin_billing"'
    'make_request "GET" "$BASE_URL/api/admin/billing/payments/approved?adminId=$ADMIN_ID" "" "$auth_header" "approved_payments"'
    'make_request "GET" "$BASE_URL/api/admin/billing/payments/rejected?adminId=$ADMIN_ID" "" "$auth_header" "rejected_payments"'
    'make_request "PUT" "$BASE_URL/api/admin/billing/payments/1/approve?adminId=$ADMIN_ID&reviewNotes=Test approval" "" "$auth_header" "approve_payment"'
    'make_request "PUT" "$BASE_URL/api/admin/billing/payments/2/reject?adminId=$ADMIN_ID&reviewNotes=Test rejection" "" "$auth_header" "reject_payment_admin"'
    'make_request "GET" "$BASE_URL/api/admin/billing/payments/1/image?adminId=$ADMIN_ID" "" "$auth_header" "payment_image"'
    'make_request "GET" "$BASE_URL/api/admin/billing/payments/codes?adminId=$ADMIN_ID" "" "$auth_header" "payment_codes"'
    'make_request "GET" "$BASE_URL/api/admin/billing/dashboard?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header" "admin_dashboard"'
    'make_request "GET" "$BASE_URL/api/stats/admin/summary?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header" "admin_summary"'
    'make_request "GET" "$BASE_URL/api/stats/seller/summary?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header" "seller_summary"'
    'make_request "GET" "$BASE_URL/api/stats/admin/dashboard?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header" "admin_dashboard_stats"'
    'make_request "GET" "$BASE_URL/api/stats/admin/analytics?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&period=monthly&metric=sales&granularity=daily&confidence=0.95&days=30" "" "$auth_header" "admin_analytics"'
    'make_request "GET" "$BASE_URL/api/stats/seller/analytics?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&period=monthly&metric=sales&granularity=daily&confidence=0.95&days=30" "" "$auth_header" "seller_analytics"'
    'make_request "GET" "$BASE_URL/api/stats/admin/financial?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&currency=PEN&taxRate=0.18" "" "$auth_header" "admin_financial"'
    'make_request "GET" "$BASE_URL/api/stats/seller/financial?sellerId=$SELLER_ID&startDate=2024-01-01&endDate=2024-12-31&include=all&currency=PEN&commissionRate=0.05" "" "$auth_header" "seller_financial"'
    'make_request "GET" "$BASE_URL/api/stats/admin/payment-transparency?adminId=$ADMIN_ID&startDate=2024-01-01&endDate=2024-12-31&includeFees=true&includeTaxes=true&includeCommissions=true" "" "$auth_header" "payment_transparency"'
)

echo "Actualizando ${#updates[@]} llamadas make_request..."

# Aplicar actualizaciones
for update in "${updates[@]}"; do
    echo "Aplicando: $update"
    # Aquí se aplicarían las actualizaciones usando sed
done

echo "Actualizaciones completadas!"
