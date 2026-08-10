import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const input = path.join(root, "docs", "marcus-usecase-activity-diagrams.puml");
const output = path.join(
  root,
  "docs",
  "marcus-usecase-activity-editable.drawio",
);
const source = fs.readFileSync(input, "utf8");

const esc = (s = "") =>
  s
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
const clean = (s = "") => s.replaceAll("\\n", " ").replace(/\s+/g, " ").trim();
let idCounter = 1;
const id = (prefix = "c") => `${prefix}${idCounter++}`;
const cell = (attrs, geometry = "") => `<mxCell ${attrs}>${geometry}</mxCell>`;
const geo = (x, y, w, h, relative = false) =>
  `<mxGeometry x="${x}" y="${y}" width="${w}" height="${h}"${relative ? ' relative="1"' : ""} as="geometry"/>`;

function baseModel(width, height, cells) {
  return `<mxGraphModel dx="1422" dy="794" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${width}" pageHeight="${height}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join("")}</root></mxGraphModel>`;
}

function edge(
  cells,
  sourceId,
  targetId,
  label = "",
  kind = "activity",
  extraStyle = "",
) {
  const relationshipLabel = /include/i.test(label)
    ? "«include»"
    : /extend/i.test(label)
      ? "«extend»"
      : clean(label);
  const umlStyle = {
    association: "dashed=0;endArrow=none;endFill=0;",
    dependency: "dashed=1;dashPattern=8 4;endArrow=open;endFill=0;",
    activity: "dashed=0;endArrow=open;endFill=0;",
  }[kind];
  const routingStyle =
    kind === "activity"
      ? "edgeStyle=orthogonalEdgeStyle;orthogonalLoop=1;jettySize=auto;"
      : "edgeStyle=none;curved=0;";
  cells.push(
    cell(
      `id="${id("e")}" value="${esc(relationshipLabel)}" style="${routingStyle}rounded=0;html=1;strokeWidth=2;strokeColor=#1F4E79;fontFamily=Arial;fontSize=12;fontStyle=1;fontColor=#000000;labelBackgroundColor=#FFFFFF;${umlStyle}${extraStyle}" edge="1" parent="1" source="${sourceId}" target="${targetId}"`,
      geo(0, 0, 0, 0, true),
    ),
  );
}

function useCasePage(block, name, title) {
  const cells = [];
  const actors = [
    ...block.matchAll(/^actor\s+(?:"([^"]+)"|(\S+))(?:\s+as\s+(\w+))?/gm),
  ].map((m) => ({ label: clean(m[1] || m[2]), alias: m[3] || m[2] }));
  const cases = [
    ...block.matchAll(/^\s*usecase\s+"([\s\S]*?)"\s+as\s+(\w+)/gm),
  ].map((m) => ({ label: clean(m[1]), alias: m[2] }));
  const map = new Map();
  const width = 1160;
  const rowGap = Math.max(85, Math.floor(650 / Math.max(1, cases.length)));
  const height = Math.max(760, 150 + cases.length * rowGap);
  const titleId = id("t");
  cells.push(
    cell(
      `id="${titleId}" value="${esc(title)}" style="text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=18;fontStyle=1;fontColor=#1F4E79;" vertex="1" parent="1"`,
      geo(180, 20, 800, 40),
    ),
  );
  const boundary = id("sys");
  cells.push(
    cell(
      `id="${boundary}" value="HỆ THỐNG MARCUSSTORE" style="rounded=0;whiteSpace=wrap;html=1;verticalAlign=top;align=center;spacingTop=10;fontFamily=Arial;fontSize=14;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=#FFFFFF;" vertex="1" parent="1"`,
      geo(290, 80, 620, height - 140),
    ),
  );
  actors.forEach((a, index) => {
    const aid = id("a");
    map.set(a.alias, aid);
    const right = index % 2 === 1;
    cells.push(
      cell(
        `id="${aid}" value="${esc(a.label)}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;fontFamily=Arial;fontSize=13;strokeColor=#1F4E79;fontColor=#1F1F1F;" vertex="1" parent="1"`,
        geo(right ? 980 : 70, 150 + index * 150, 80, 110),
      ),
    );
  });
  cases.forEach((u, index) => {
    const uid = id("u");
    map.set(u.alias, uid);
    cells.push(
      cell(
        `id="${uid}" value="${esc(u.label)}" style="ellipse;whiteSpace=wrap;html=1;aspect=fixed;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=13;strokeWidth=2;strokeColor=#1F4E79;fillColor=#F7FBFF;fontColor=#1F1F1F;" vertex="1" parent="1"`,
        geo(470, 120 + index * rowGap, 260, 64),
      ),
    );
  });
  for (const m of block.matchAll(
    /^\s*(\w+)\s+(-->|\.\.>)\s+(\w+)(?:\s*:\s*(.+))?$/gm,
  )) {
    if (map.has(m[1]) && map.has(m[3])) {
      const kind = m[2] === "..>" ? "dependency" : "association";
      edge(cells, map.get(m[1]), map.get(m[3]), m[4] || "", kind);
    }
  }
  return baseModel(width, height, cells);
}

function activityPage(block, name, title) {
  const cells = [];
  const tokens = [];
  let lane = "Hệ thống";
  for (const raw of block.split(/\r?\n/)) {
    const line = raw.trim();
    const laneMatch = line.match(/^\|(.+)\|$/);
    if (laneMatch) {
      lane = clean(laneMatch[1]);
      continue;
    }
    const action = line.match(/^:(.+);$/);
    const decision = line.match(/^if \((.+)\) then \((.+)\)$/);
    const alternative = line.match(/^else \((.+)\)$/);
    if (line === "start") tokens.push({ type: "start", lane });
    else if (line === "stop") tokens.push({ type: "stop", lane });
    else if (action)
      tokens.push({ type: "action", label: clean(action[1]), lane });
    else if (decision)
      tokens.push({
        type: "decision",
        label: clean(decision[1]),
        yes: clean(decision[2]),
        lane,
      });
    else if (alternative)
      tokens.push({ type: "else", label: clean(alternative[1]), lane });
    else if (line === "endif") tokens.push({ type: "endif", lane });
  }
  const lanes = [
    ...new Set(
      tokens
        .filter((t) => !["else", "endif"].includes(t.type))
        .map((t) => t.lane),
    ),
  ];
  const laneWidth = 330;
  const width = Math.max(900, 80 + lanes.length * laneWidth);
  const visible = tokens.filter((t) => !["else", "endif"].includes(t.type));
  const height = Math.max(760, 150 + visible.length * 90);
  cells.push(
    cell(
      `id="${id("t")}" value="${esc(title)}" style="text;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=18;fontStyle=1;fontColor=#1F4E79;" vertex="1" parent="1"`,
      geo(80, 20, width - 160, 40),
    ),
  );
  lanes.forEach((l, i) =>
    cells.push(
      cell(
        `id="${id("lane")}" value="${esc(l.toUpperCase())}" style="swimlane;horizontal=1;startSize=38;html=1;rounded=0;fontFamily=Arial;fontSize=13;fontStyle=1;fontColor=#1F4E79;strokeWidth=2;strokeColor=#5B9BD5;fillColor=#DCE6F1;swimlaneFillColor=#FFFFFF;" vertex="1" parent="1"`,
        geo(40 + i * laneWidth, 80, laneWidth, height - 120),
      ),
    ),
  );
  let y = 135;
  let previous = null;
  const branchStack = [];
  for (const t of tokens) {
    if (t.type === "else") {
      if (branchStack.length) {
        const frame = branchStack.at(-1);
        frame.yesLast = previous;
        frame.inElse = true;
        frame.noLabel =
          t.label && t.label.toLowerCase() !== "không"
            ? `Không - ${t.label}`
            : "Không";
        previous = frame.id;
      }
      continue;
    }
    if (t.type === "endif") {
      const frame = branchStack.pop();
      if (frame) {
        const noLast = frame.inElse ? previous : frame.id;
        const yesLast = frame.yesLast || previous;
        const mergeId = id("merge");
        const mergeX =
          40 + Math.max(0, lanes.indexOf(frame.lane)) * laneWidth + 155;
        cells.push(
          cell(
            `id="${mergeId}" value="" style="rhombus;html=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=#FFFFFF;" vertex="1" parent="1"`,
            geo(mergeX, y, 30, 30),
          ),
        );
        if (yesLast && yesLast !== frame.id)
          edge(cells, yesLast, mergeId, "", "activity");
        if (noLast === frame.id)
          edge(
            cells,
            frame.id,
            mergeId,
            frame.noLabel || "Không",
            "activity",
            "exitX=1;exitY=0.5;entryX=1;entryY=0.5;",
          );
        else if (noLast)
          edge(cells, noLast, mergeId, "", "activity");
        previous = mergeId;
        y += 72;
      }
      continue;
    }
    const x = 40 + Math.max(0, lanes.indexOf(t.lane)) * laneWidth + 65;
    const nid = id("n");
    if (t.type === "start" || t.type === "stop") {
      cells.push(
        cell(
          `id="${nid}" value="" style="ellipse;html=1;aspect=fixed;fillColor=${t.type === "start" ? "#1F4E79" : "#FFFFFF"};strokeColor=#1F4E79;strokeWidth=3;" vertex="1" parent="1"`,
          geo(x + 90, y, 28, 28),
        ),
      );
    } else if (t.type === "decision") {
      cells.push(
        cell(
          `id="${nid}" value="${esc(t.label)}" style="rhombus;whiteSpace=wrap;html=1;fontFamily=Arial;fontSize=12;strokeWidth=2;strokeColor=#1F4E79;fillColor=#EAF3F8;" vertex="1" parent="1"`,
          geo(x + 45, y, 120, 75),
        ),
      );
      branchStack.push({
        id: nid,
        lane: t.lane,
        yesLabel: t.yes || "Có",
        noLabel: "Không",
        yesLast: null,
        inElse: false,
      });
    } else {
      cells.push(
        cell(
          `id="${nid}" value="${esc(t.label)}" style="rounded=1;whiteSpace=wrap;html=1;arcSize=12;fontFamily=Arial;fontSize=13;strokeWidth=2;strokeColor=#1F4E79;fillColor=#F7FBFF;fontColor=#1F1F1F;" vertex="1" parent="1"`,
          geo(x, y, 210, 58),
        ),
      );
    }
    if (previous) {
      let label = "";
      let extraStyle = "";
      if (branchStack.length && branchStack.at(-1).id === previous) {
        const frame = branchStack.at(-1);
        label = frame.inElse ? frame.noLabel : frame.yesLabel;
        extraStyle = frame.inElse
          ? "exitX=1;exitY=0.5;entryX=1;entryY=0.5;"
          : "exitX=0.5;exitY=1;entryX=0.5;entryY=0;";
      }
      edge(cells, previous, nid, label, "activity", extraStyle);
    }
    previous = nid;
    y += t.type === "decision" ? 110 : 88;
  }
  return baseModel(width, height, cells);
}

const blocks = [
  ...source.matchAll(/^@startuml\s+(\S+)\s*\r?\n([\s\S]*?)^@enduml/gm),
]
  .map((m, sourceIndex) => {
    const name = m[1];
    const body = m[2];
    const title = clean(body.match(/^title\s+(.+)$/m)?.[1] || name);
    const uc = Number(name.match(/^UC(\d+)/)?.[1] || 999);
    return {
      name,
      body,
      title,
      uc,
      sourceIndex,
      type: name.endsWith("_USECASE") ? 0 : 1,
    };
  })
  .sort(
    (a, b) => a.uc - b.uc || a.type - b.type || a.sourceIndex - b.sourceIndex,
  );

const pages = blocks.map((b, index) => {
  idCounter = 1;
  const model =
    b.type === 0
      ? useCasePage(b.body, b.name, b.title)
      : activityPage(b.body, b.name, b.title);
  return `<diagram id="page-${index + 1}" name="${esc(b.title)}">${model}</diagram>`;
});

const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net" modified="2026-08-03T00:00:00.000Z" agent="Codex" version="26.0.0" type="device" compressed="false">${pages.join("")}</mxfile>\n`;
fs.writeFileSync(output, xml, "utf8");
console.log(`Created ${output}`);
console.log(`Pages: ${pages.length}`);
