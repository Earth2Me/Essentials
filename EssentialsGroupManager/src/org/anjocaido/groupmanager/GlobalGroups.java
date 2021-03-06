package org.anjocaido.groupmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.utils.PermissionCheckResult;
import org.anjocaido.groupmanager.utils.Tasks;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * @author ElgarL
 * 
 */
public class GlobalGroups {

	private GroupManager plugin;
	//private Yaml GGroups;

	private final Map<String, Group> groups = Collections.synchronizedMap(new HashMap<String, Group>());

	protected long timeStampGroups = 0;
	protected boolean haveGroupsChanged = false;
	protected File GlobalGroupsFile = null;

	public GlobalGroups(GroupManager plugin) {

		this.plugin = plugin;
		load();
	}

	/**
	 * @return the haveGroupsChanged
	 */
	public boolean haveGroupsChanged() {

		if (this.haveGroupsChanged) {
			return true;
		}
		synchronized(groups) {
		for (Group g : groups.values()) {
			if (g.isChanged()) {
				return true;
			}
		}
		}
		return false;
	}

	/**
	 * @return the timeStampGroups
	 */
	public long getTimeStampGroups() {

		return timeStampGroups;
	}

	/**
	 * @param timeStampGroups the timeStampGroups to set
	 */
	protected void setTimeStampGroups(long timeStampGroups) {

		this.timeStampGroups = timeStampGroups;
	}

	/**
	 * @param haveGroupsChanged the haveGroupsChanged to set
	 */
	public void setGroupsChanged(boolean haveGroupsChanged) {

		this.haveGroupsChanged = haveGroupsChanged;
	}

	/**
	 * Loads data from YAML
	 */
	@SuppressWarnings("unchecked")
	public void load() {

		Yaml GGroupYAML = new Yaml(new SafeConstructor());
		Map<String, Object> GGroups;
		
		GroupManager.setLoaded(false);

		// READ globalGroups FILE
		if (GlobalGroupsFile == null)
			GlobalGroupsFile = new File(plugin.getDataFolder(), "globalgroups.yml");

		if (!GlobalGroupsFile.exists()) {
			try {
				// Create a new file if it doesn't exist.
				Tasks.copy(plugin.getResourceAsStream("globalgroups.yml"), GlobalGroupsFile);
			} catch (IOException ex) {
				GroupManager.logger.log(Level.SEVERE, null, ex);
			}
		}

		/*
		 * Load the YAML file.
		 */
		try {
			FileInputStream groupsInputStream = new FileInputStream(GlobalGroupsFile);
			GGroups = (Map<String, Object>) GGroupYAML.load(new UnicodeReader(groupsInputStream));
			groupsInputStream.close();
		} catch (Exception ex) {
			throw new IllegalArgumentException("The following file couldn't pass on Parser.\n" + GlobalGroupsFile.getPath(), ex);
		}

		// Clear out old groups
		resetGlobalGroups();

		if (!GGroups.keySet().isEmpty()) {
			// Read all global groups
			Map<String, Object> allGroups = new HashMap<String, Object>();

			try {
				allGroups = (Map<String, Object>) GGroups.get("groups");
			} catch (Exception ex) {
				// ex.printStackTrace();
				throw new IllegalArgumentException("Your " + GlobalGroupsFile.getPath() + " file is invalid. See console for details.", ex);
			}

			// Load each groups permissions list.
			if (allGroups != null) {

				Iterator<String> groupItr = allGroups.keySet().iterator();
				String groupName;
				Integer groupCount = 0;

				/*
				 * loop each group entry
				 * and read it's data.
				 */
				while (groupItr.hasNext()) {
					try {
						groupCount++;
						// Attempt to fetch the next group name.
						groupName = groupItr.next();
					} catch (Exception ex) {
						throw new IllegalArgumentException("Invalid group name for GlobalGroup entry (" + groupCount + ") in file: " + GlobalGroupsFile.getPath(), ex);
					}

					/*
					 * Create a new group with this name.
					 */
					Group newGroup = new Group(groupName.toLowerCase());
					Object element;

					// Permission nodes
					try {
						element = ((Map<String, Object>)allGroups.get(groupName)).get("permissions");
					} catch ( Exception ex) {
						throw new IllegalArgumentException("The GlobalGroup ' " + groupName + "' is formatted incorrectly: ", ex);
					}

					if (element != null)
						if (element instanceof List) {
							try {
								for (String node : (List<String>) element) {
									if ((node != null) && !node.isEmpty())
										newGroup.addPermission(node);
								}
							} catch (ClassCastException ex) {
								throw new IllegalArgumentException("Invalid permission node for global group:  " + groupName, ex);
							}
						} else if (element instanceof String) {
							if ((element != null) && !((String)element).isEmpty())
							newGroup.addPermission((String) element);
						} else
							throw new IllegalArgumentException("Unknown type of permission node for global group:  " + groupName);

//					// Info nodes
//					try {
//						element = ((Map<String, Object>)allGroups.get(groupName)).get("info");
//					} catch ( Exception ex) {
//						throw new IllegalArgumentException("The GlobalGroup ' " + groupName + "' is formatted incorrectly: ", ex);
//					}
//
//					if (element != null)
//						if (element instanceof MemorySection) {
//							Map<String, Object> vars = new HashMap<String, Object>();
//							for (String key : ((MemorySection) element).getKeys(false)) {
//								vars.put(key, ((MemorySection) element).get(key));
//							}
//							newGroup.setVariables(vars);
//						} else
//							throw new IllegalArgumentException("Unknown type of info node for global group:  " + groupName);

					// Push a new group
					addGroup(newGroup);
				}
			}

			removeGroupsChangedFlag();
		}

		setTimeStampGroups(GlobalGroupsFile.lastModified());
		GroupManager.setLoaded(true);
		// GlobalGroupsFile = null;
	}

	/**
	 * Write the globalgroups.yml file
	 *
	 * @param overwrite true to overwrite
	 */

	public void writeGroups(boolean overwrite) {

		// File GlobalGroupsFile = new File(plugin.getDataFolder(), "globalgroups.yml");

		if (haveGroupsChanged()) {
			if (overwrite || (!overwrite && (getTimeStampGroups() >= GlobalGroupsFile.lastModified()))) {
				Map<String, Object> root = new HashMap<String, Object>();

				Map<String, Object> groupsMap = new HashMap<String, Object>();
				root.put("groups", groupsMap);
				synchronized(groups) {
				for (String groupKey : groups.keySet()) {
					Group group = groups.get(groupKey);

					// Group header
					Map<String, Object> aGroupMap = new HashMap<String, Object>();
					groupsMap.put(group.getName(), aGroupMap);

//					// Info nodes
//					Map<String, Object> infoMap = new HashMap<String, Object>();
//					aGroupMap.put("info", infoMap);
//
//					for (String infoKey : group.getVariables().getVarKeyList()) {
//						infoMap.put(infoKey, group.getVariables().getVarObject(infoKey));
//					}

					// Permission nodes
					aGroupMap.put("permissions", group.getPermissionList());
				}
				}

				if (!root.isEmpty()) {
					DumperOptions opt = new DumperOptions();
					opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
					final Yaml yaml = new Yaml(opt);
					try {
						yaml.dump(root, new OutputStreamWriter(new FileOutputStream(GlobalGroupsFile), "UTF-8"));
					} catch (UnsupportedEncodingException ex) {
					} catch (FileNotFoundException ex) {
					}
				}
				setTimeStampGroups(GlobalGroupsFile.lastModified());
			} else {
				// Newer file found.
				GroupManager.logger.log(Level.WARNING, "Newer GlobalGroups file found, but we have local changes!");
				throw new IllegalStateException("Unable to save unless you issue a '/mansave force'");
			}
			removeGroupsChangedFlag();
		} else {
			// Check for newer file as no local changes.
			if (getTimeStampGroups() < GlobalGroupsFile.lastModified()) {
				System.out.print("Newer GlobalGroups file found (Loading changes)!");
				// Backup GlobalGroups file
				backupFile();
				load();
			}
		}

	}

	/**
	 * Backup the BlobalGroups file
	 */
	private void backupFile() {

		File backupFile = new File(plugin.getBackupFolder(), "bkp_ggroups_" + Tasks.getDateString() + ".yml");
		try {
			Tasks.copy(GlobalGroupsFile, backupFile);
		} catch (IOException ex) {
			GroupManager.logger.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Adds a group, or replaces an existing one.
	 * 
	 * @param groupToAdd group to add
	 */
	public void addGroup(Group groupToAdd) {

		// Create a new group if it already exists
		if (hasGroup(groupToAdd.getName())) {
			groupToAdd = groupToAdd.clone();
			removeGroup(groupToAdd.getName());
		}

		newGroup(groupToAdd);
		haveGroupsChanged = true;
		if (GroupManager.isLoaded())
			GroupManager.getGMEventHandler().callEvent(groupToAdd, GMGroupEvent.Action.GROUP_ADDED);
	}

	/**
	 * Creates a new group if it doesn't already exist.
	 * 
	 * @param newGroup new group
	 * @return the newly created group
	 */
	public Group newGroup(Group newGroup) {

		// Push a new group
		if (!groups.containsKey(newGroup.getName().toLowerCase())) {
			groups.put(newGroup.getName().toLowerCase(), newGroup);
			this.setGroupsChanged(true);
			return newGroup;
		}
		return null;
	}

	/**
	 * Delete a group if it exist.
	 * 
	 * @param groupName group name
	 * @return whether the group existed
	 */
	public boolean removeGroup(String groupName) {

		// Push a new group
		if (groups.containsKey(groupName.toLowerCase())) {
			groups.remove(groupName.toLowerCase());
			this.setGroupsChanged(true);
			if (GroupManager.isLoaded())
				GroupManager.getGMEventHandler().callEvent(groupName.toLowerCase(), GMGroupEvent.Action.GROUP_REMOVED);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the Global Group exists in the globalgroups.yml
	 * 
	 * @param groupName group name
	 * @return true if the group exists
	 */
	public boolean hasGroup(String groupName) {

		return groups.containsKey(groupName.toLowerCase());
	}

	/**
	 * Returns true if the group has the correct permission node.
	 * 
	 * @param groupName group name
	 * @param permissionNode permission node
	 * @return true if node exists
	 */
	public boolean hasPermission(String groupName, String permissionNode) {

		if (!hasGroup(groupName))
			return false;

		return groups.get(groupName.toLowerCase()).hasSamePermissionNode(permissionNode);

	}

	/**
	 * Returns a PermissionCheckResult of the permission node for the group to
	 * be tested against.
	 * 
	 * @param groupName group name
	 * @param permissionNode permission node
	 * @return PermissionCheckResult object
	 */
	public PermissionCheckResult checkPermission(String groupName, String permissionNode) {

		PermissionCheckResult result = new PermissionCheckResult();
		result.askedPermission = permissionNode;
		result.resultType = PermissionCheckResult.Type.NOTFOUND;

		if (!hasGroup(groupName))
			return result;

		Group tempGroup = groups.get(groupName.toLowerCase());

		if (tempGroup.hasSamePermissionNode(permissionNode))
			result.resultType = PermissionCheckResult.Type.FOUND;
		if (tempGroup.hasSamePermissionNode("-" + permissionNode))
			result.resultType = PermissionCheckResult.Type.NEGATION;
		if (tempGroup.hasSamePermissionNode("+" + permissionNode))
			result.resultType = PermissionCheckResult.Type.EXCEPTION;

		return result;
	}

	/**
	 * Returns a List of all permission nodes for this group null if none
	 * 
	 * @param groupName group name
	 * @return List of all group names
	 */
	public List<String> getGroupsPermissions(String groupName) {

		if (!hasGroup(groupName))
			return null;

		return groups.get(groupName.toLowerCase()).getPermissionList();
	}

	/**
	 * Returns a Set of all global group names.
	 * 
	 * @return Set containing all group names.
	 */
	/*public Set<String> getGlobalGroups() {

		return groups.keySet();
	}*/

	/**
	 * Resets GlobalGroups.
	 */
	public void resetGlobalGroups() {
		this.groups.clear();
	}

	/**
	 * Gets a collection of groups
	 * 
	 * @return a collection of the groups
	 */
	public Group[] getGroupList() {
		synchronized(groups) {
			return groups.values().toArray(new Group[0]);
		}
	}

	/**
	 * Returns the Global Group or null if it doesn't exist.
	 * 
	 * @param groupName group name
	 * @return Group object
	 */
	public Group getGroup(String groupName) {

		if (!hasGroup(groupName))
			return null;

		return groups.get(groupName.toLowerCase());

	}

	/**
	 * @return the globalGroupsFile
	 */
	public File getGlobalGroupsFile() {

		return GlobalGroupsFile;
	}

	/**
     * Marks all groups as saved
     */
	public void removeGroupsChangedFlag() {

		setGroupsChanged(false);
		synchronized(groups) {
		for (Group g : groups.values()) {
			g.flagAsSaved();
		}
		}
	}

}