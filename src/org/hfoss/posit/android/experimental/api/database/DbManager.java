/*
 * File: AcdiVocaDbHelper.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */

package org.hfoss.posit.android.experimental.api.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.User;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaMessage;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaUser;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * The class is the interface with the Database. It controls all Db access and
 * directly handles all Db queries.
 */
public class DbManager extends OrmLiteSqliteOpenHelper {

	private static final String TAG = "DbHelper";

	protected static final String DATABASE_NAME = "posit";
	public static final int DATABASE_VERSION = 2;

	public static final int DELETE_FIND = 1;
	public static final int UNDELETE_FIND = 0;
	public static final String WHERE_NOT_DELETED = " " + AcdiVocaFind.DELETED
			+ " != " + DELETE_FIND + " ";
	public static final String DATETIME_NOW = "`datetime('now')`";

	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
	public static final String HISTORY_ID = "_id";

	// DAO objects used to access the Db tables
	private Dao<User, Integer> userDao = null;
	private Dao<Find, Integer> findDao = null;


	/**
	 * Constructor just saves and opens the Db.
	 * 
	 * @param context
	 */
	public DbManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * Invoked automatically if the Database does not exist.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		Log.i(TAG, "onCreate");
		Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
		try {
			findClass.getMethod("createTable", ConnectionSource.class).invoke(null, connectionSource);
		} catch (Exception e) {
			e.printStackTrace();
		} 		
		User.createTable(connectionSource, getUserDao());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource,
			int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "onUpgrade");
			TableUtils.dropTable(connectionSource, User.class, true);
			
			Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
			TableUtils.dropTable(connectionSource, findClass, true);
			
			// after we drop the old databases, we create the new ones
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches all finds currently in the database.
	 * @return A list of all the finds.
	 */
	public List<? extends Find> getAllFinds() {
		List<Find> list = null;
		try {
			list = getFindDao().queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}

	/**
	 * Looks up a find by its ID.
	 * @param id the id of the find to look up
	 * @return the find
	 */
	public Find getFindById(int id) {
		Find find = null;
		try {
			find = getFindDao().queryForId(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return find;
	}
	
	public int updateStatus(Find find, int status) {
		int rows = 0;
		try {
			find.setStatus(status);
			rows = getFindDao().update(find);
			if (rows == 1)
				Log.i(TAG, "Updated find status:  " + this.toString());
			else {
				Log.e(TAG, "Db Error updating find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}


	public int updateSyncOperation(Find find, int operation) {
		int rows = 0;
		try {
			find.setSyncOperation(operation);
			rows = getFindDao().update(find);
			if (rows == 1)
				Log.i(TAG, "Updated find sync operation:  " + this.toString());
			else {
				Log.e(TAG, "Db Error updating sync operation in find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. It
	 * will create it or just give the cached value.
	 */
	public Dao<User, Integer> getUserDao() {
		if (userDao == null) {
			try {
				userDao = getDao(User.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return userDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the Find class. It
	 * will create it or just give the cached value.
	 */
	public Dao<Find, Integer> getFindDao() {
		if (findDao == null) {
			Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
			try {
				findDao = getDao(findClass);
			} catch (SQLException e) {
				Log.e(TAG, "Get find DAO failed.");
				e.printStackTrace();
			}
		}
		return findDao;
	}



	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		userDao = null;
		findDao = null;
	}

}