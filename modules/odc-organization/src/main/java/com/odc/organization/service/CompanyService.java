package com.odc.organization.service;

import com.odc.organization.db.Company;

public interface CompanyService {

  Company save(Company company);

  void validate(Company company);

  void archive(Company company);

  void requireUsable(Company company);
}
