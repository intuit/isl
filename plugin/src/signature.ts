import * as vscode from 'vscode';
import { getModifiersMap, getFunctionsByNamespace } from './language';

export class IslSignatureHelpProvider implements vscode.SignatureHelpProvider {
    
    provideSignatureHelp(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken,
        context: vscode.SignatureHelpContext
    ): vscode.SignatureHelp | undefined {
        const line = document.lineAt(position).text;
        const beforeCursor = line.substring(0, position.character);
        
        // Check if we're inside a modifier call
        const modifierMatch = beforeCursor.match(/\|\s*([a-zA-Z_][a-zA-Z0-9_.]*)(?:\(([^)]*))?$/);
        if (modifierMatch) {
            return this.getModifierSignature(modifierMatch[1], beforeCursor);
        }
        
        // Check if we're inside a service call
        const serviceMatch = beforeCursor.match(/@\.([A-Z][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)(?:\(([^)]*))?$/);
        if (serviceMatch) {
            return this.getServiceSignature(serviceMatch[1], serviceMatch[2], beforeCursor);
        }
        
        // Check if we're inside a custom function call (@.This.)
        const thisMatch = beforeCursor.match(/@\.This\.([a-zA-Z_][a-zA-Z0-9_]*)(?:\(([^)]*))?$/);
        if (thisMatch) {
            return this.getCustomFunctionSignature(document, thisMatch[1], beforeCursor);
        }
        
        return undefined;
    }
    
    private getModifierSignature(modifier: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const modifiersMap = getModifiersMap();
        const mod = modifiersMap.get(modifier);
        if (!mod?.signature) {
            return undefined;
        }
        const sig = mod.signature;
        const signatureHelp = new vscode.SignatureHelp();
        const signature = new vscode.SignatureInformation(
            sig.label,
            new vscode.MarkdownString(sig.documentation ?? '')
        );
        (sig.parameters ?? []).forEach(param => {
            signature.parameters.push(
                new vscode.ParameterInformation(param.label, new vscode.MarkdownString(param.documentation ?? ''))
            );
        });
        signatureHelp.signatures = [signature];
        signatureHelp.activeSignature = 0;
        const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
        const commaCount = (paramSection.match(/,/g) || []).length;
        signatureHelp.activeParameter = Math.min(commaCount, (sig.parameters ?? []).length - 1);
        return signatureHelp;
    }
    
    private getServiceSignature(service: string, method: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const byNamespace = getFunctionsByNamespace();
        const methods = byNamespace.get(service);
        if (!methods) return undefined;
        const func = methods.find(f => f.name.toLowerCase() === method.toLowerCase());
        if (!func?.signature) return undefined;
        const sig = func.signature;
        const signatureHelp = new vscode.SignatureHelp();
        const signature = new vscode.SignatureInformation(
            sig.label,
            new vscode.MarkdownString(sig.documentation ?? '')
        );
        (sig.parameters ?? []).forEach(param => {
            signature.parameters.push(
                new vscode.ParameterInformation(param.label, new vscode.MarkdownString(param.documentation ?? ''))
            );
        });
        signatureHelp.signatures = [signature];
        signatureHelp.activeSignature = 0;
        const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
        const commaCount = (paramSection.match(/,/g) || []).length;
        signatureHelp.activeParameter = Math.min(commaCount, (sig.parameters ?? []).length - 1);
        return signatureHelp;
    }
    
    private getCustomFunctionSignature(document: vscode.TextDocument, functionName: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Find function declaration
        const functionPattern = new RegExp(`^\\s*(fun|modifier)\\s+${functionName}\\s*\\(([^)]*)\\)`);
        
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(functionPattern);
            if (match) {
                const funcType = match[1];
                const params = match[2].trim();
                
                if (!params) {
                    return undefined; // No parameters
                }
                
                const signatureHelp = new vscode.SignatureHelp();
                const signature = new vscode.SignatureInformation(
                    `${funcType} ${functionName}(${params})`,
                    new vscode.MarkdownString(`Custom ${funcType} defined in this file`)
                );
                
                // Parse parameters
                const paramList = params.split(',').map(p => p.trim()).filter(p => p.length > 0);
                paramList.forEach(param => {
                    const paramName = param.split(':')[0].trim();
                    signature.parameters.push(new vscode.ParameterInformation(paramName));
                });
                
                signatureHelp.signatures = [signature];
                signatureHelp.activeSignature = 0;
                
                // Determine active parameter
                const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
                const commaCount = (paramSection.match(/,/g) || []).length;
                signatureHelp.activeParameter = Math.min(commaCount, paramList.length - 1);
                
                return signatureHelp;
            }
        }
        
        return undefined;
    }
}

