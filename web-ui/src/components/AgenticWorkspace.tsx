import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import { Bot, Send, User, Download, FileSpreadsheet, FileText, Sparkles, ThumbsUp, ThumbsDown, Check, Loader2 } from 'lucide-react';
import { marked } from 'marked';
import * as XLSX from 'xlsx';
import { Document, Packer, Paragraph, TextRun } from 'docx';
import html2pdf from 'html2pdf.js';

interface ChatMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  riskLevel?: string;
  recommendations?: string[];
  timestamp: string;
  originalQuery?: string;
}

const QUICK_PROMPTS = [
  'Generate tabular report of all items',
  'Which items are low in stock?',
  'What is our total portfolio valuation?',
  'Show warehouse distribution breakdown',
  'Identify overstocked and dead stock risks',
];

export const AgenticWorkspace: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'agent',
      text: 'Hello! I am your **Agentic AI Inventory Operations Manager**. Ask me anything about stock telemetry, warehouse distributions, low-stock reorder points, or financial portfolio valuations.',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [feedbackDone, setFeedbackDone] = useState<Record<string, boolean>>({});

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const handleSend = async (queryText?: string) => {
    const q = (queryText || input).trim();
    if (!q || loading) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: q,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!queryText) setInput('');
    setLoading(true);

    try {
      const response = await axios.post('/api/ai/query', { query: q });
      const data = response.data || {};
      const agentMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'agent',
        text: data.answer || 'No operational summary produced.',
        riskLevel: data.riskLevel || data.risk_level || 'LOW',
        recommendations: data.recommendations || [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        originalQuery: q,
      };
      setMessages((prev) => [...prev, agentMsg]);
    } catch (err: any) {
      const errorMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'agent',
        text: `⚠️ **AI Query Error**: ${err.response?.data?.message || err.response?.data?.error || 'Failed to communicate with AI forecasting service.'}`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const handleFeedback = async (msgId: string, originalQuery: string, rating: 'THUMBS_UP' | 'THUMBS_DOWN') => {
    try {
      await axios.post('/api/ai/feedback', {
        originalQuery,
        rating,
        correctedIntent: 'CUSTOM_SORT_FILTER',
      });
      setFeedbackDone((prev) => ({ ...prev, [msgId]: true }));
    } catch (err) {
      console.error('Feedback submission failed', err);
    }
  };

  const exportExcel = (text: string) => {
    const parser = new DOMParser();
    const doc = parser.parseFromString(marked.parse(text) as string, 'text/html');
    const table = doc.querySelector('table');
    if (!table) {
      alert('No data table found in this AI response to export.');
      return;
    }
    const wb = XLSX.utils.table_to_book(table);
    XLSX.writeFile(wb, 'Inventory_AI_Report.xlsx');
  };

  const exportWord = async (text: string) => {
    const doc = new Document({
      sections: [
        {
          children: [
            new Paragraph({
              children: [new TextRun({ text: 'Smart Inventory - Agentic AI Report', bold: true, size: 28 })],
            }),
            new Paragraph({
              children: [new TextRun({ text: text.replace(/<[^>]+>/g, ''), size: 22 })],
            }),
          ],
        },
      ],
    });
    const blob = await Packer.toBlob(doc);
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'Inventory_AI_Report.docx';
    link.click();
  };

  const exportPdf = (msgId: string) => {
    const element = document.getElementById(`msg-content-${msgId}`);
    if (!element) return;
    html2pdf().from(element).save('Inventory_AI_Report.pdf');
  };

  return (
    <div className="flex flex-col h-[calc(100vh-8.5rem)] rounded-2xl bg-slate-900/80 border border-slate-800 shadow-2xl overflow-hidden">
      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex items-start gap-3.5 ${msg.sender === 'user' ? 'flex-row-reverse' : ''}`}
          >
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 font-bold text-xs shadow-md ${
                msg.sender === 'user'
                  ? 'bg-sky-500 text-white shadow-sky-500/20'
                  : 'bg-gradient-to-tr from-indigo-500 to-purple-600 text-white shadow-indigo-500/20'
              }`}
            >
              {msg.sender === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
            </div>

            <div className={`max-w-3xl space-y-2 ${msg.sender === 'user' ? 'items-end' : ''}`}>
              <div className="flex items-center gap-2 text-[11px] text-slate-400">
                <span className="font-semibold text-slate-300">
                  {msg.sender === 'user' ? 'You' : 'Agentic Assistant'}
                </span>
                <span>{msg.timestamp}</span>
                {msg.riskLevel && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                    RISK: {msg.riskLevel}
                  </span>
                )}
              </div>

              <div
                id={`msg-content-${msg.id}`}
                className={`p-4 rounded-2xl text-sm leading-relaxed overflow-x-auto ${
                  msg.sender === 'user'
                    ? 'bg-sky-600 text-white rounded-tr-none shadow-lg'
                    : 'bg-slate-950 border border-slate-800 text-slate-200 rounded-tl-none shadow-md prose prose-invert max-w-none'
                }`}
                dangerouslySetInnerHTML={{ __html: marked.parse(msg.text) as string }}
              />

              {/* Recommendations */}
              {msg.recommendations && msg.recommendations.length > 0 && (
                <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 space-y-1">
                  <span className="text-[11px] font-bold text-indigo-400 flex items-center gap-1">
                    <Sparkles className="w-3 h-3" /> Operational Recommendations
                  </span>
                  <ul className="text-xs text-slate-300 list-disc list-inside space-y-0.5">
                    {msg.recommendations.map((rec, i) => (
                      <li key={i}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Action bar for agent messages */}
              {msg.sender === 'agent' && msg.id !== 'welcome' && (
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={() => exportExcel(msg.text)}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-slate-700"
                  >
                    <FileSpreadsheet className="w-3 h-3 text-emerald-400" /> Excel
                  </button>
                  <button
                    onClick={() => exportWord(msg.text)}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-slate-700"
                  >
                    <FileText className="w-3 h-3 text-sky-400" /> Word
                  </button>
                  <button
                    onClick={() => exportPdf(msg.id)}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-slate-700"
                  >
                    <Download className="w-3 h-3 text-purple-400" /> PDF
                  </button>

                  <div className="h-3 w-[1px] bg-slate-800 mx-1" />

                  {feedbackDone[msg.id] ? (
                    <span className="text-[10px] text-emerald-400 flex items-center gap-1">
                      <Check className="w-3 h-3" /> Feedback Received
                    </span>
                  ) : (
                    <>
                      <button
                        onClick={() => handleFeedback(msg.id, msg.originalQuery || '', 'THUMBS_UP')}
                        className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-emerald-400"
                      >
                        <ThumbsUp className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleFeedback(msg.id, msg.originalQuery || '', 'THUMBS_DOWN')}
                        className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-rose-400"
                      >
                        <ThumbsDown className="w-3.5 h-3.5" />
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Loading / Thinking Indicator */}
        {loading && (
          <div className="flex items-start gap-3.5">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 font-bold text-xs bg-gradient-to-tr from-indigo-500 to-purple-600 text-white shadow-indigo-500/20">
              <Bot className="w-4 h-4" />
            </div>
            <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 text-slate-400 text-xs flex items-center gap-2 rounded-tl-none shadow-md">
              <Loader2 className="w-4 h-4 animate-spin text-sky-400" />
              <span>Agentic AI is querying inventory telemetry & analyzing forecasting models...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Quick Prompts Bar */}
      <div className="px-6 py-2.5 bg-slate-950/60 border-t border-slate-800 flex items-center gap-2 overflow-x-auto no-scrollbar">
        <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0 flex items-center gap-1">
          <Sparkles className="w-3 h-3 text-sky-400" /> Quick Prompts:
        </span>
        {QUICK_PROMPTS.map((prompt, idx) => (
          <button
            key={idx}
            onClick={() => handleSend(prompt)}
            disabled={loading}
            className="px-3 py-1 rounded-full bg-slate-900 hover:bg-sky-500/15 text-slate-300 hover:text-sky-300 text-xs font-medium border border-slate-800 hover:border-sky-500/30 shrink-0 transition-all disabled:opacity-50"
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Input Form Bar */}
      <div className="p-4 bg-slate-950 border-t border-slate-800">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="flex items-center gap-3"
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask the Agentic AI Assistant..."
            className="flex-1 px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-slate-100 text-sm focus:outline-none focus:border-sky-500 transition-colors"
          />
          <button
            type="submit"
            disabled={loading || !input.trim()}
            className="px-5 py-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-bold text-xs flex items-center gap-2 shadow-lg shadow-sky-500/20 disabled:opacity-50"
          >
            <Send className="w-4 h-4" />
            Send
          </button>
        </form>
      </div>
    </div>
  );
};
