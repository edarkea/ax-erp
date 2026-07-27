package com.odc.organization.context;

import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;

public record OrganizationContextResolution(
    OrganizationContextStatus status, Company company, Branch branch) {}
