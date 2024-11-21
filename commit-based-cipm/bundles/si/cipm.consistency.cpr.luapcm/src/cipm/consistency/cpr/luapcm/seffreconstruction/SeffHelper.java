package cipm.consistency.cpr.luapcm.seffreconstruction;

import org.apache.log4j.Logger;

public class SeffHelper {
    private static final Logger LOGGER = Logger.getLogger(SeffHelper.class.getName());

    /**
     * This tests for null, i.e. returns false if the given functionDeclaration is null.
     * @param functionDeclaration
     * @return
     */
    public static boolean needsSeffReconstruction(LuaFunctionDeclaration functionDeclaration) {
        if (functionDeclaration == null) {
            return false;
        }

        var infos = ComponentSetInfoRegistry.getInfosForComponentSet(functionDeclaration.getRoot());
        var needsSeff = infos.needsSeffReconstruction(functionDeclaration);
        if (needsSeff) {
            LOGGER.trace(String.format("Statement_Function_Declaration needs SEFF reconstruction: %s",
                    functionDeclaration.getName()));
        }
        return needsSeff;
    }

}
