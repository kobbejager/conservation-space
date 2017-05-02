package com.sirma.itt.seip.permissions.rest;

import static com.sirma.itt.seip.domain.instance.DefaultProperties.INHERIT_LIBRARY_PERMISSIONS;
import static com.sirma.itt.seip.domain.instance.DefaultProperties.INHERIT_PARENT_PERMISSIONS;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.ObjectUtils;

import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.domain.instance.InstanceReference;
import com.sirma.itt.seip.domain.instance.InstanceType;
import com.sirma.itt.seip.domain.security.ActionTypeConstants;
import com.sirma.itt.seip.instance.InstanceTypeResolver;
import com.sirma.itt.seip.instance.InstanceTypes;
import com.sirma.itt.seip.instance.context.InstanceContextInitializer;
import com.sirma.itt.seip.permissions.InstanceAccessEvaluator;
import com.sirma.itt.seip.permissions.PermissionModelType;
import com.sirma.itt.seip.permissions.PermissionService;
import com.sirma.itt.seip.permissions.SecurityModel;
import com.sirma.itt.seip.permissions.action.AuthorityService;
import com.sirma.itt.seip.permissions.role.EntityPermission;
import com.sirma.itt.seip.permissions.role.EntityPermissionDao;
import com.sirma.itt.seip.permissions.role.PermissionsChange;
import com.sirma.itt.seip.permissions.role.PermissionsChange.PermissionsChangeBuilder;
import com.sirma.itt.seip.permissions.role.ResourceRole;
import com.sirma.itt.seip.permissions.role.RoleAssignments;
import com.sirma.itt.seip.permissions.role.RoleIdentifier;
import com.sirma.itt.seip.permissions.role.RoleService;
import com.sirma.itt.seip.rest.exceptions.ResourceException;
import com.sirma.itt.seip.rest.models.ErrorData;
import com.sirma.itt.seip.rest.utils.JsonKeys;
import com.sirma.itt.seip.rest.utils.Versions;
import com.sirma.itt.seip.security.exception.NoPermissionsException;

/**
 * The {@link InstancePermissionsRestService} provides rest access to the permission backend support
 *
 * @author BBonev
 */
@ApplicationScoped
@Path("/instances")
@Produces(Versions.V2_JSON)
public class InstancePermissionsRestService {

	@Inject
	private PermissionService permissionService;

	@Inject
	private RoleService roleService;

	@Inject
	private AuthorityService authorityService;

	@Inject
	private InstanceTypeResolver typeResolver;

	@Inject
	private InstanceContextInitializer contextInitializer;

	@Inject
	private InstanceAccessEvaluator instanceAccessEvaluator;

	@Inject
	private InstanceTypes instanceTypes;

	@Inject
	private EntityPermissionDao entityPermissionDao;

	/**
	 * Load permissions information for an instance. The includeParentPermissions and includeLibraryPermissions params
	 * can be used to calculate the permissions in a different way then the currently persisted one. This can be used
	 * for dry-run pre-calculations without touching the database (i.e. to precalculate the permissions for the user to
	 * see what would happen when a flag is turned on or off).
	 *
	 * @param instanceId
	 *            the instance id
	 * @param includeParentPermissions
	 *            whether to include the parent permissions of not
	 * @param includeLibraryPermissions
	 *            whether to include the library permissions or not
	 * @return the permissions
	 */
	@GET
	@Path("{id}/permissions")
	public Permissions loadPermissions(@PathParam(JsonKeys.ID) String instanceId,
			@QueryParam("includeInherited") Boolean includeParentPermissions,
			@QueryParam("includeLibrary") Boolean includeLibraryPermissions) {

		InstanceReference reference = getReferenceOrThrowError(instanceId);
		// CS-1399 set up context parent
		contextInitializer.restoreHierarchy(reference);
		return loadPermissionsForInstance(reference, includeParentPermissions, includeLibraryPermissions);
	}

	private Permissions loadPermissionsForInstance(InstanceReference reference, Boolean includeParentPermissions,
			Boolean includeLibraryPermissions) {
		Set<String> actionNames = authorityService.getAllowedActionNames(reference.toInstance(), null);

		boolean editActionAllowed = actionNames.contains(ActionTypeConstants.MANAGE_PERMISSIONS);
		boolean restorePermissionsActionAllowed = actionNames.contains(ActionTypeConstants.RESTORE_PERMISSIONS);

		boolean isRoot = permissionService.checkIsRoot(reference);

		PermissionModelType permissionModelType = permissionService.getPermissionModel(reference);

		Permissions permissions = new Permissions();
		permissions.setEditAllowed(editActionAllowed);
		permissions.setRestoreAllowed(restorePermissionsActionAllowed);
		permissions.setRoot(isRoot);
		permissions.setReference(reference);

		// if the manual filters are enabled use them in the response instead of the persisted flags
		permissions.setInheritedPermissions(
				ObjectUtils.defaultIfNull(includeParentPermissions, permissionModelType.isInherited()));
		permissions.setInheritedLibraryPermissions(
				ObjectUtils.defaultIfNull(includeLibraryPermissions, permissionModelType.isLibrary()));

		if (reference.getType().is("classinstance")) {
			InstanceType classInstance = instanceTypes.from(reference.getIdentifier()).get();
			permissions.setAllowInheritParentPermissions(classInstance.hasTrait(INHERIT_PARENT_PERMISSIONS));
			permissions.setAllowInheritLibraryPermissions(classInstance.hasTrait(INHERIT_LIBRARY_PERMISSIONS));
		}

		// if nothing is passed to includeParentPermissions means to get all permissions
		Map<String, ResourceRole> permissionAssignments = permissionService.getPermissionAssignments(reference,
				includeParentPermissions, includeLibraryPermissions);

		for (Entry<String, ResourceRole> assignment : permissionAssignments.entrySet()) {
			RoleAssignments roleAssignments = assignment.getValue().getRoleAssignments();
			PermissionEntry entry = permissions.getForAuthority(assignment.getKey());

			entry.setAuthority(assignment.getValue().getAuthorityId());
			entry.setSpecial(roleAssignments.getSpecial());
			entry.setInherited(roleAssignments.getInherited());
			entry.setLibrary(roleAssignments.getLibrary());
			entry.setCalculated(roleAssignments.getActive());
			if (roleAssignments.isManager()) {
				entry.setManager(null);
			}
		}

		return permissions;
	}

	/**
	 * Save permission changes and returns the permissions after change.
	 * <p>
	 * The expected input format is
	 *
	 * <pre>
	 * <code>
	 * {
	 *   "permissions": [{
	 *       "id": "user identifier",
	 *       "special": "CONSUMER"
	 *   },
	 *   {
	 *       "id": "other user identifier",
	 *       "special": "COLLABORATOR"
	 *   }],
	 *   "inheritedPermissionsEnabled": true
	 * }</code>
	 * </pre>
	 *
	 * @param instanceId
	 *            the instance id
	 * @param permissions
	 *            the permissions
	 * @return the permissions after the change
	 */
	@POST
	@Path("{id}/permissions")
	@Transactional(TxType.REQUIRED)
	public Permissions save(@PathParam(JsonKeys.ID) String instanceId, Permissions permissions) {

		InstanceReference reference = getReferenceOrThrowError(instanceId);
		contextInitializer.restoreHierarchy(reference);

		if (!isEditAllowed(reference)) {
			throw new NoPermissionsException(instanceId, "Not allowed to change permissions");
		}

		// FIXME: the change set should be generated by the client
		Collection<PermissionsChange> changes = generateChangeSet(reference, permissions);
		permissionService.setPermissions(reference, changes);

		return loadPermissionsForInstance(reference, permissions.isInheritedPermissions(),
				permissions.isInheritedLibraryPermissions());
	}

	private Collection<PermissionsChange> generateChangeSet(InstanceReference reference, Permissions permissions) {
		// if the current instance does not have any permissions then
		// the input changes will become the active permissions
		EntityPermission entityPermission = entityPermissionDao
				.loadWithAssignments(reference.getIdentifier())
					.orElseGet(() -> new EntityPermission().setTargetId(reference.getIdentifier()));

		PermissionsChangeBuilder builder = PermissionsChange.builder();

		Map<String, RoleIdentifier> updatedAssignments = permissions.getRoleMapping(roleService::getRoleIdentifier);

		// find added/updated
		updatedAssignments
				.entrySet()
					.stream()
					.filter(entry -> !containsAssignment(entityPermission, entry.getKey(),
							entry.getValue().getIdentifier()))
					.forEach(entry -> {
						builder.addRoleAssignmentChange(entry.getKey(), entry.getValue().getIdentifier());
					});

		// find removed
		entityPermission
				.getAssignments()
					.stream()
					.filter(assignment -> !updatedAssignments.containsKey(assignment.getAuthority()))
					.forEach(assignment -> {
						builder.removeRoleAssignmentChange(assignment.getAuthority(), assignment.getRole());
					});

		// inherit from parent
		if (permissions.isInheritedPermissions() != entityPermission.getInheritFromParent()) {
			builder.inheritFromParentChange(permissions.isInheritedPermissions());
		}

		// inherit from library
		if (permissions.isInheritedLibraryPermissions() != entityPermission.getInheritFromLibrary()) {
			builder.inheritFromLibraryChange(permissions.isInheritedLibraryPermissions());
		}

		return builder.build();
	}

	private static boolean containsAssignment(EntityPermission entityPermission, String authority, String role) {
		return entityPermission
				.getAssignments()
					.stream()
					.filter(assignment -> assignment.getAuthority().equals(authority))
					.filter(assignment -> assignment.getRole().equals(role))
					.findAny()
					.isPresent();
	}

	private boolean isEditAllowed(InstanceReference reference) {
		return instanceAccessEvaluator.isAtLeastRole(reference, SecurityModel.BaseRoles.COLLABORATOR);
	}

	/**
	 * Prepares permission restore operation and returns the number of affected entities. Entities are only scheduled
	 * for restore, a POST to /instances/{id}/permission/restore?execute must be called to actually restore the
	 * permissions. <br>
	 * Restores the inherited permissions for each instance processed by the
	 * {@link PermissionService#restoreInheritedPermissions(Instance)}.
	 *
	 * @param instanceId
	 *            the target instance id.
	 * @param shouldExecute
	 *            if present the restore operation will be executed
	 * @return JSON containing the number for the scheduled entries
	 */
	@POST
	@Path("{id}/permissions/restore-from-parent")
	@Transactional(TxType.REQUIRED)
	public void restorePermissions(@PathParam(JsonKeys.ID) String instanceId) {
		InstanceReference reference = getReferenceOrThrowError(instanceId);
		permissionService.restoreParentPermissions(reference);
	}

	/**
	 * Retrieves all roles in the system.
	 *
	 * @return json array containing all roles.
	 */
	@GET
	@Path("/permissions/roles")
	public Collection<RoleIdentifier> getActiveRoles() {
		return roleService.getActiveRoles();
	}

	private InstanceReference getReferenceOrThrowError(String instanceId) {
		return typeResolver.resolveReference(instanceId).orElseThrow(() -> new ResourceException(Status.NOT_FOUND,
				new ErrorData("Instance " + instanceId + " not found"), null));
	}
}
