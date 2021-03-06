package com.chrisfolger.needsmoredojo.core.amd;

import com.chrisfolger.needsmoredojo.core.util.DeclareUtil;
import com.chrisfolger.needsmoredojo.core.util.DefineUtil;
import com.intellij.lang.javascript.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.util.List;

/**
 Parse the AMD imports in a dojo module according to this syntax:

 define([modules], function(modules) {});

 */
public class DefineResolver
{
    private Logger logger = Logger.getLogger(DefineResolver.class);

    public JSRecursiveElementVisitor getDefineAndParametersVisitor( final List<PsiElement> defines, final List<PsiElement> parameters, final PsiElementVisitor defineVisitor)
    {
        return new JSRecursiveElementVisitor() {
            @Override
            public void visitJSCallExpression(JSCallExpression element)
            {
                // if the user entered invalid syntax we don't want to account for every case, so just catch and log it
                try
                {
                    JSExpression[] arguments = element.getArguments();
                    if(!element.getMethodExpression().getText().equals("define"))
                    {
                        return;
                    }

                    DefineUtil.DefineStatementItems items = new DefineUtil().getDefineStatementItemsFromArguments(arguments);
                    if(items == null)
                    {
                        super.visitJSCallExpression(element);
                        return;
                    }

                    // get the first argument which should be an array literal
                    JSArrayLiteralExpression literalExpressions = items.getArguments();
                    for(JSExpression expression : literalExpressions.getExpressions())
                    {
                        if(expression instanceof JSLiteralExpression)
                        {
                            JSLiteralExpression literal = (JSLiteralExpression) expression;
                            defines.add(literal);
                        }
                    }

                    // get the second argument which should be a function
                    JSFunctionExpression function = items.getFunction();
                    for(JSParameter parameter : function.getFunction().getParameters())
                    {
                        parameters.add(parameter);
                    }
                }
                catch(Exception e)
                {
                    logger.log(Priority.INFO, "exception ecountered in DefineResolver ", e);
                }

                super.visitJSCallExpression(element);
            }
        };
    }

    public JSRecursiveElementVisitor getDefineAndParametersVisitor( final List<PsiElement> defines, final List<PsiElement> parameters)
    {
        return getDefineAndParametersVisitor(defines, parameters, null);
    }

    public void gatherDefineAndParameters(PsiFile psiFile, final List<PsiElement> defines, final List<PsiElement> parameters)
    {
        psiFile.accept(getDefineAndParametersVisitor(defines, parameters));
    }
}
