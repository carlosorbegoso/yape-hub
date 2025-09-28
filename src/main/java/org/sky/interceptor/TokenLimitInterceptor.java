package org.sky.interceptor;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.sky.annotation.TokenConsumption;
import org.sky.exception.InsufficientTokensException;
import org.sky.service.TokenService;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;

@Interceptor
@TokenConsumption
public class TokenLimitInterceptor {

    @Inject
    TokenService tokenService;

    @AroundInvoke
    public Object checkTokenLimits(InvocationContext context) {
        Method method = context.getMethod();
        TokenConsumption annotation = method.getAnnotation(TokenConsumption.class);

        if(annotation == null) {
            return  processAsync(context);
        }

        Long adminId = extractAdminIdFromContext(context);
            if (adminId == null) {
              return processAsync(context);
            }

          return tokenService.consumeTokens(adminId, annotation.operationType(), annotation.tokens())
              .onItem().transformToUni(success ->
                  Boolean.TRUE.equals(success) ? processAsync(context)
                  : Uni.createFrom().failure(
                      new InsufficientTokensException("Tokens insufficient")
                  ));

    }

    Uni<Object> processAsync(InvocationContext context){
      try{
        return Uni.createFrom().item(context.proceed());
      }catch (Exception e){
        return Uni.createFrom().failure(e);
      }
    }

  private Long extractAdminIdFromContext(InvocationContext context) {
    try {
      return Arrays.stream(context.getParameters())
          .filter(Long.class::isInstance)
          .map(Long.class::cast)
          .findFirst()
          .orElseGet(() -> {
            Log.warn("Could not extract adminId automatically from context");
            return null;
          });
    } catch (Exception e) {
      Log.error("Error extracting adminId from context: " + e.getMessage());
      return null;
    }
  }


}
