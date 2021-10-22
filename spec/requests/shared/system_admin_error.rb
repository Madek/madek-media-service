RSpec.configure do |c|
  c.alias_it_should_behave_like_to :it_raises
end

shared_examples "system admin error" do
  let(:api_token) do
    create(:api_token, user: user, scope_write: true) if user
  end
  let(:user_token) { api_token&.token_hash }

  it "responds with 403 Forbidden status" do
    expect(response.status).to eq(403)
  end

  it "responds with error message" do
    expect(response.body).to include("System-admin scope required")
  end
end
