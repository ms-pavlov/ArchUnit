package org.example.arch;

import com.tngtech.archunit.core.importer.Location;

public class ExcludeTestsImportOption implements com.tngtech.archunit.core.importer.ImportOption {

    @Override
    public boolean includes(Location location) {
        return !location.contains("arch");
    }

}
