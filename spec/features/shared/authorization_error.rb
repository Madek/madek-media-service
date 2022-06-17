RSpec.configure do |c|
  c.alias_it_should_behave_like_to :it_displays
end

shared_examples "authorization error" do
  it "displays error" do
    visit path
    within(".modal") do
      expect(page).to have_css(".modal-header", text: "Request ERROR 403")
      expect(page.find(".modal-body")).to have_content("system-admin")
    end
  end
end
