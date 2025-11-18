package pers.sanne.my_batis_go;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import pers.sanne.my_batis_go.references.MyBatisXmlReference;
import pers.sanne.my_batis_go.utils.CommonUtil;

public class JavaJumpMybatisReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new JavaLiteralReferenceProvider()
        );
    }

    /**
     * Reference provider for Java literal expressions.
     */
    public static class JavaLiteralReferenceProvider extends PsiReferenceProvider {

        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element,
                @NotNull ProcessingContext context) {

            if (!(element instanceof PsiLiteralExpression literalExpression)) {
                return PsiReference.EMPTY_ARRAY;
            }

            Object rawValue = literalExpression.getValue();
            if (!(rawValue instanceof String value)) {
                return PsiReference.EMPTY_ARRAY;
            }

            // must be "xxx.yyy"
            if (!value.contains(".")) {
                return PsiReference.EMPTY_ARRAY;
            }

            String[] parts = value.split("\\.");
            if (parts.length != 2) {
                return PsiReference.EMPTY_ARRAY;
            }

            // find enclosing method call: xxx.method("mapper.id")
            PsiMethodCallExpression methodCallExpression =
                    PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
            if (methodCallExpression == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            PsiMethod method = methodCallExpression.resolveMethod();
            if (method == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            // skip CommonRepository subclasses
            if (!CommonUtil.isICommonRepositorySubclass(containingClass)) {
                return PsiReference.EMPTY_ARRAY;
            }

            // build reference
            return new PsiReference[]{
                    new MyBatisXmlReference(literalExpression, parts[0], parts[1])
            };
        }
    }
}
