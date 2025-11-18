package pers.sanne.my_batis_go.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.xml.XmlAttributeValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class JavaOperatorReference extends PsiPolyVariantReferenceBase<XmlAttributeValue> {
    private static final Logger logger = Logger.getInstance(JavaOperatorReference.class);
    private final String namespace;
    private final String queryId;

    public JavaOperatorReference(@NotNull XmlAttributeValue element, @NotNull String namespace, @NotNull String queryId) {
        super(element, new TextRange(1, element.getTextLength() - 1));
        this.namespace = namespace;
        this.queryId = queryId;
    }

    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        List<PsiLiteralExpression> result = new ArrayList<>();
        Project project = this.myElement.getProject();
        this.getPsiLiteralExpression(project, result);
        return result.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    private void getPsiLiteralExpression(Project project, List<PsiLiteralExpression> result) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            logger.error("Project base path is null");
        }

        String targetPath = basePath + "/src/main/java/com/dongni";
        VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (directory == null || !directory.isDirectory()) {
            logger.error("Target directory does not exist or is not a directory: " + targetPath);
        }

        GlobalSearchScope scope = new GlobalSearchScopesCore.DirectoryScope(project, directory, true);
        PsiSearchHelper instance = PsiSearchHelper.getInstance(project);
        instance.processElementsWithWord((element, offsetInElement) -> {
            if (element instanceof PsiLiteralExpression expression) {
                if (Objects.equals(expression.getValue(), this.namespace + "." + this.queryId)) {
                    result.add(expression);
                }
            }

            return true;
        }, scope, this.namespace + "." + this.queryId, (short)255, false);
    }
}
