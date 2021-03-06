package com.knightonline.shared.persistence.dao;

import com.knightonline.shared.persistence.entities.Account;

/**
 * @author Mamaorha
 *
 */
public interface IAccountDAO extends IGenericDAO<Account, Long>
{
	public Account getAccountByUsername(String username);
	public short accountLoginProcedure(String username, String password);
}
