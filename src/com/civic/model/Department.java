package com.civic.model;

import java.util.ArrayList;
import java.util.List;

public class Department {
    private String id;
    private String name;
    private String code;
    private String categoryKey;
    private String icon;
    private String headOfficer;
    private String contactEmail;
    private String phone;
    private int activeOfficers;

    public Department() {}

    public Department(String id, String name, String code, String categoryKey, String icon, String headOfficer, String contactEmail, String phone, int activeOfficers) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.categoryKey = categoryKey;
        this.icon = icon;
        this.headOfficer = headOfficer;
        this.contactEmail = contactEmail;
        this.phone = phone;
        this.activeOfficers = activeOfficers;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getHeadOfficer() { return headOfficer; }
    public void setHeadOfficer(String headOfficer) { this.headOfficer = headOfficer; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getActiveOfficers() { return activeOfficers; }
    public void setActiveOfficers(int activeOfficers) { this.activeOfficers = activeOfficers; }

    public static List<Department> getDefaultDepartments() {
        List<Department> list = new ArrayList<>();
        list.add(new Department("dept-roads", "Roads & Highway Infrastructure", "ROADS", "ROADS", "road", "Eng. Rajesh Verma", "roads@civic-response.gov", "+91 11-2345-6701", 14));
        list.add(new Department("dept-water", "Water Supply & Sewerage Board", "WATER", "WATER_DRAINAGE", "droplet", "Director S. Sundaram", "water@civic-response.gov", "+91 11-2345-6702", 18));
        list.add(new Department("dept-sanitation", "Solid Waste Management & Sanitation", "SANITATION", "SANITATION", "trash-2", "Chief Officer Anita Sen", "waste@civic-response.gov", "+91 11-2345-6703", 26));
        list.add(new Department("dept-electric", "Public Lighting & Power Grid", "ELECTRIC", "ELECTRICITY", "zap", "Supt. Eng. Vikram Patel", "power@civic-response.gov", "+91 11-2345-6704", 12));
        list.add(new Department("dept-safety", "Public Safety & Disaster Relief", "SAFETY", "PUBLIC_SAFETY", "shield-alert", "Cmdr. Meera Rao", "safety@civic-response.gov", "+91 11-2345-6705", 9));
        list.add(new Department("dept-parks", "Urban Parks & Green Environment", "PARKS", "PARKS", "trees", "Horticulturist P. Nair", "parks@civic-response.gov", "+91 11-2345-6706", 8));
        return list;
    }
}
