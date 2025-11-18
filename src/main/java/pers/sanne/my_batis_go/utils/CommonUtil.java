package pers.sanne.my_batis_go.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommonUtil {
    private static final Set<String> MYBATIS_OPERATOR_SET = new HashSet<>(Arrays.asList("insert", "update", "delete", "select"));

    public static boolean containMybatisOperator(String operator) {
        return MYBATIS_OPERATOR_SET.contains(operator);
    }

    public static boolean isICommonRepositorySubclass(PsiClass psiClass) {
        if (psiClass == null) return false;

        PsiClass curr = psiClass;

        while (curr != null) {
            for (PsiClassType type : curr.getSuperTypes()) {
                PsiClass resolved = type.resolve();
                if (resolved == null) continue;

                // 直接或间接继承 ICommonRepository
                if ("com.dongni.commons.repository.ICommonRepository".equals(resolved.getQualifiedName())) {
                    return true;
                }

                // 递归检查父接口
                if (isICommonRepositorySubclass(resolved)) {
                    return true;
                }
            }
            curr = curr.getSuperClass();
        }
        return false;
    }
}