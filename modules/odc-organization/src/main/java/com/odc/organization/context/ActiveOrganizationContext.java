package com.odc.organization.context;

import com.axelor.auth.db.User;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;

public record ActiveOrganizationContext(User user, Company company, Branch branch) {}
