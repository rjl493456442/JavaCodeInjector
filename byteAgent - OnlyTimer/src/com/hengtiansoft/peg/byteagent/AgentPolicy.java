package com.hengtiansoft.peg.byteagent;

/**
 * Created by jialeirong on 2/1/2016.
 */

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;

final class AgentPolicy extends Policy {

    private static final AllPermission ALL_PERMISSION = new AllPermission();

    static final Permissions DEFAULT_PERMISSION_COLLECTION = getAllPermission();

    private static final CodeSource ourCodeSource = AgentPolicy.class.getProtectionDomain().getCodeSource();

    private final Policy policy;

    private static Permissions getAllPermission() {
        final Permissions permissions = new Permissions();
        permissions.add(ALL_PERMISSION);
        return permissions;
    }

    public AgentPolicy(final Policy policy) {
        this.policy = policy;
    }

    public Provider getProvider() {
        return policy.getProvider();
    }

    public String getType() {
        return policy.getType();
    }

    public Parameters getParameters() {
        return policy.getParameters();
    }

    public PermissionCollection getPermissions(final CodeSource codesource) {
        return codesource.equals(ourCodeSource) ? getAllPermission() : policy.getPermissions(codesource);
    }

    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        return domain.getCodeSource().equals(ourCodeSource) ? getAllPermission() : policy.getPermissions(domain);
    }

    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        return domain.getCodeSource().equals(ourCodeSource) || policy.implies(domain, permission);
    }

    public void refresh() {
        policy.refresh();
    }
}