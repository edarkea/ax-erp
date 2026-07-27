package com.odc.organization.service;

import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;

public interface BranchService {

  Branch save(Branch branch);

  void validate(Branch branch);

  void validate(Branch branch, Company owningCompany);

  void setDefault(Branch branch);

  void archive(Branch branch);

  void requireUsable(Branch branch);
}
