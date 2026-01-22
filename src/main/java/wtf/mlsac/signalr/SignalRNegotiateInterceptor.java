/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package wtf.mlsac.signalr;

import okhttp3.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * OkHttp interceptor that converts GET requests to /negotiate endpoint into POST requests.
 * This is required for compatibility with SignalR Core (ASP.NET Core version) which only
 * accepts POST for the negotiate endpoint, while the Java SignalR client library defaults to GET.
 */
public class SignalRNegotiateInterceptor implements Interceptor {
    
    private static final String NEGOTIATE_PATH = "/negotiate";
    private final Logger logger;
    private final boolean debug;
    
    public SignalRNegotiateInterceptor(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        HttpUrl url = originalRequest.url();
        
        // Check if this is a negotiate request
        if (url.encodedPath().endsWith(NEGOTIATE_PATH) && "GET".equals(originalRequest.method())) {
            if (debug) {
                logger.info("[SignalR] Converting GET negotiate request to POST for ASP.NET Core compatibility");
            }
            
            // Convert GET to POST with empty body
            Request newRequest = originalRequest.newBuilder()
                .method("POST", RequestBody.create(new byte[0], null))
                .build();
            
            return chain.proceed(newRequest);
        }
        
        // For all other requests, proceed normally
        return chain.proceed(originalRequest);
    }
}
