/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.management.handlers.management.api.resources;

import io.gravitee.am.identityprovider.api.User;
import io.gravitee.am.management.handlers.management.api.security.Permission;
import io.gravitee.am.management.handlers.management.api.security.Permissions;
import io.gravitee.am.model.Policy;
import io.gravitee.am.model.permissions.RolePermission;
import io.gravitee.am.model.permissions.RolePermissionAction;
import io.gravitee.am.service.DomainService;
import io.gravitee.am.service.PolicyService;
import io.gravitee.am.service.exception.DomainNotFoundException;
import io.gravitee.am.service.exception.PolicyNotFoundException;
import io.gravitee.am.service.model.UpdatePolicy;
import io.gravitee.common.http.MediaType;
import io.reactivex.Maybe;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private DomainService domainService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a policy")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Policy", response = Policy.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.DOMAIN_EXTENSION_POINT, acls = RolePermissionAction.READ)
    })
    public void get(
            @PathParam("domain") String domain,
            @PathParam("policy") String policy,
            @Suspended final AsyncResponse response) {
        domainService.findById(domain)
                .switchIfEmpty(Maybe.error(new DomainNotFoundException(domain)))
                .flatMap(irrelevant -> policyService.findById(policy))
                .switchIfEmpty(Maybe.error(new PolicyNotFoundException(policy)))
                .map(policy1 -> {
                    if (!policy1.getDomain().equalsIgnoreCase(domain)) {
                        throw new BadRequestException("Policy does not belong to domain");
                    }
                    return Response.ok(policy1).build();
                })
                .subscribe(
                        result -> response.resume(result),
                        error -> response.resume(error));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a policy")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Policy successfully updated", response = Policy.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.DOMAIN_EXTENSION_POINT, acls = RolePermissionAction.UPDATE)
    })
    public void update(
            @PathParam("domain") String domain,
            @PathParam("policy") String policy,
            @ApiParam(name = "identity", required = true) @Valid @NotNull UpdatePolicy updatePolicy,
            @Suspended final AsyncResponse response) {
        final User authenticatedUser = getAuthenticatedUser();

        domainService.findById(domain)
                .switchIfEmpty(Maybe.error(new DomainNotFoundException(domain)))
                .flatMapSingle(irrelevant -> policyService.update(domain, policy, updatePolicy, authenticatedUser))
                .map(policy1 -> Response.ok(policy1).build())
                .subscribe(
                        result -> response.resume(result),
                        error -> response.resume(error));
    }

    @DELETE
    @ApiOperation(value = "Delete a policy")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Policy successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.DOMAIN_EXTENSION_POINT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("domain") String domain,
                       @PathParam("policy") String policy,
                       @Suspended final AsyncResponse response) {
        final User authenticatedUser = getAuthenticatedUser();

        policyService.delete(policy, authenticatedUser)
                .subscribe(
                        () -> response.resume(Response.noContent().build()),
                        error -> response.resume(error));
    }
}
