package cipm.consistency.cpr.luapcm.seffreconstruction;

import org.apache.log4j.Logger;

import lua.Referenceable;

public class SeffHelper {
    private static final Logger LOGGER = Logger.getLogger(SeffHelper.class.getName());

    public static boolean needsSeffReconstruction(Referenceable declaration) {
        if (declaration == null) {
            return false;
        }

        var infos = ComponentSetInfoRegistry.getInfosForComponentSet(declaration);
        var needsSeff = infos.needsSeffReconstruction(declaration);
        if (needsSeff) {
            LOGGER.trace(String.format("Statement_Function_Declaration needs SEFF reconstruction: %s",
                    declaration.getName()));
        }
        return needsSeff;
    }

}
