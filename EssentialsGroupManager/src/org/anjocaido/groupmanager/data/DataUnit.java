/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.utils.StringPermissionComparator;

/**
 * 
 * @author gabrielcouto
 */
public abstract class DataUnit {

	private WorldDataHolder dataSource;
	private String uUID;
	private String lastName;
	private boolean changed, sorted = false;
	private List<String> permissions = Collections.unmodifiableList(Collections.<String>emptyList());

	public DataUnit(WorldDataHolder dataSource, String name) {

		this.dataSource = dataSource;
		this.uUID = name;
	}

	public DataUnit(String name) {

		this.uUID = name;
	}

	/**
	 * Every group is matched only by their names and DataSources names.
	 * 
	 * @param o object
	 * @return true if they are equal. false if not.
	 */
	@Override
	public boolean equals(Object o) {

		if (o instanceof DataUnit) {
			DataUnit go = (DataUnit) o;
			if (this.getUUID().equalsIgnoreCase(go.getUUID())) {
				// Global Group match.
				if (this.dataSource == null && go.getDataSource() == null)
					return true;
				// This is a global group, the object to test isn't.
				if (this.dataSource == null && go.getDataSource() != null)
					return false;
				// This is not a global group, but the object to test is.
				if (this.dataSource != null && go.getDataSource() == null)
					return false;
				// Match on group name and world name.
				if (this.dataSource.getName().equalsIgnoreCase(go.getDataSource().getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {

		int hash = 5;
		hash = 71 * hash + (this.uUID != null ? this.uUID.toLowerCase().hashCode() : 0);
		return hash;
	}

	/**
	 * Set the data source to point to a different worldDataHolder
	 * 
	 * @param source world data holder
	 */
	public void setDataSource(WorldDataHolder source) {

		this.dataSource = source;
	}

	/**
	 * Get the current worldDataHolder this object is pointing to
	 * 
	 * @return the dataSource
	 */
	public WorldDataHolder getDataSource() {

		return dataSource;
	}
	
	public String getUUID() {

		return uUID;
	}
	
	public String getLastName() {

		if (uUID.length() < 36)
			return this.uUID;
		
		return this.lastName;
	}
	
	public void setLastName(String lastName) {

		if (!lastName.equals(this.lastName)) {
			
			this.lastName = lastName;
			changed = true;
			
		}
		
	}

	public void flagAsChanged() {

		WorldDataHolder testSource = getDataSource();
		String source = "";

		if (testSource == null)
			source = "GlobalGroups";
		else
			source = testSource.getName();

		GroupManager.logger.finest("DataSource: " + source + " - DataUnit: " + getUUID() + " flagged as changed!");
		// for(StackTraceElement st: Thread.currentThread().getStackTrace()){
		// GroupManager.logger.finest(st.toString());
		// }
		sorted = false;
		changed = true;
	}

	public boolean isChanged() {

		return changed;
	}

	public void flagAsSaved() {

		WorldDataHolder testSource = getDataSource();
		String source = "";

		if (testSource == null)
			source = "GlobalGroups";
		else
			source = testSource.getName();

		GroupManager.logger.finest("DataSource: " + source + " - DataUnit: " + getUUID() + " flagged as saved!");
		changed = false;
	}

	public boolean hasSamePermissionNode(String permission) {

		return permissions.contains(permission);
	}

	public void addPermission(String permission) {

		if (!hasSamePermissionNode(permission)) {
			List<String> clone = new ArrayList<String>(permissions);
			clone.add(permission);
			permissions = Collections.unmodifiableList(clone);
		}
		flagAsChanged();
	}

	public boolean removePermission(String permission) {

		flagAsChanged();
		List<String> clone = new ArrayList<String>(permissions);
		boolean ret = clone.remove(permission);
		permissions = Collections.unmodifiableList(clone);
		return ret;
	}

	/**
	 * Use this only to list permissions.
	 * You can't edit the permissions using the returned ArrayList instance
	 * 
	 * @return a copy of the permission list
	 */
	public List<String> getPermissionList() {
		sortPermissions();
		return permissions;
	}

	public boolean isSorted() {

		return this.sorted;
	}

	public void sortPermissions() {

		if (!isSorted()) {
			List<String> clone = new ArrayList<String>(permissions);
			Collections.sort(clone, StringPermissionComparator.getInstance());
			permissions = Collections.unmodifiableList(clone);
			sorted = true;
		}
	}
}